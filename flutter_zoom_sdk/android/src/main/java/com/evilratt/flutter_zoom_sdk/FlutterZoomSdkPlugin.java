package com.evilratt.flutter_zoom_sdk;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import us.zoom.sdk.ChatMessageDeleteType;
import us.zoom.sdk.CustomizedNotificationData;
import us.zoom.sdk.FreeMeetingNeedUpgradeType;
import us.zoom.sdk.InMeetingAudioController;
import us.zoom.sdk.InMeetingChatController;
import us.zoom.sdk.InMeetingChatMessage;
import us.zoom.sdk.InMeetingEventHandler;
import us.zoom.sdk.InMeetingNotificationHandle;
import us.zoom.sdk.InMeetingService;
import us.zoom.sdk.InMeetingServiceListener;
import us.zoom.sdk.InviteOptions;
import us.zoom.sdk.JoinMeetingOptions;
import us.zoom.sdk.JoinMeetingParams;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.MeetingViewsOptions;
import us.zoom.sdk.MobileRTCSDKError;
import us.zoom.sdk.StartMeetingOptions;
import us.zoom.sdk.StartMeetingParams4NormalUser;
import us.zoom.sdk.VideoQuality;
import us.zoom.sdk.ZoomAuthenticationError;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomSDKAuthenticationListener;
import us.zoom.sdk.ZoomSDKInitParams;
import us.zoom.sdk.ZoomSDKInitializeListener;
import us.zoom.sdk.ZoomSDKRawDataMemoryMode;
import us.zoom.sdk.IRequestLocalRecordingPrivilegeHandler;

/** FlutterZoomPlugin */
public class FlutterZoomSdkPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

  private MethodChannel methodChannel;
  private Context context;
  private EventChannel meetingStatusChannel;
  private InMeetingService inMeetingService;
  private InMeetingServiceListener mInMeetingServiceListener;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    context = flutterPluginBinding.getApplicationContext();
    methodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "com.evilratt/zoom_sdk");
    methodChannel.setMethodCallHandler(this);

    meetingStatusChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "com.evilratt/zoom_sdk_event_stream");
  }

  @Override
  public void onMethodCall(@NonNull MethodCall methodCall, @NonNull MethodChannel.Result result) {
    switch (methodCall.method) {
      case "init":
        init(methodCall, result);
        break;
      case "switchZoomOEM":
        switchZoomOEM(methodCall, result);
        break;
      case "login":
        login(methodCall, result);
        break;
      case "logout":
        logout();
        break;
      case "join":
        joinMeeting(methodCall, result);
        break;
      case "startNormal":
        startMeetingNormal(methodCall, result);
        break;
      case "meeting_status":
        meetingStatus(result);
        break;
      case "meeting_details":
        meetingDetails(result);
        break;
      default:
        result.notImplemented();
    }

  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    methodChannel.setMethodCallHandler(null);
  }

  //Initializing the Zoom SDK for Android
  private void init(final MethodCall methodCall, final MethodChannel.Result result) {

    Map<String, String> options = methodCall.arguments();

    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if(zoomSDK.isInitialized()) {
      List<Integer> response = Arrays.asList(0, 0);
      result.success(response);
      return;
    }

    ZoomSDKInitParams initParams = new ZoomSDKInitParams();
    initParams.appKey = options.get("appKey");
    initParams.appSecret = options.get("appSecret");
    initParams.domain = options.get("domain");
    initParams.enableLog = true;

    final InMeetingNotificationHandle handle=new InMeetingNotificationHandle() {

      @Override
      public boolean handleReturnToConfNotify(Context context, Intent intent) {
        intent = new Intent(context, FlutterZoomSdkPlugin.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        if(context == null) {
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.setAction(InMeetingNotificationHandle.ACTION_RETURN_TO_CONF);
        assert context != null;
        context.startActivity(intent);
        return true;
      }
    };

    //Set custom Notification fro android
    final CustomizedNotificationData data = new CustomizedNotificationData();
    data.setContentTitleId(R.string.app_name_zoom_local);
    data.setLargeIconId(R.drawable.zm_mm_type_emoji);
    data.setSmallIconId(R.drawable.zm_mm_type_emoji);
    data.setSmallIconForLorLaterId(R.drawable.zm_mm_type_emoji);

    ZoomSDKInitializeListener listener = new ZoomSDKInitializeListener() {
      /**
       * @param errorCode {@link us.zoom.sdk.ZoomError#ZOOM_ERROR_SUCCESS} if the SDK has been initialized successfully.
       */
      @Override
      public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
        List<Integer> response = Arrays.asList(errorCode, internalErrorCode);

        if (errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
          System.out.println("Failed to initialize Zoom SDK: + errorCode = " + errorCode + ", internalErrorCode = " + internalErrorCode);
          result.success(response);
          return;
        }

        ZoomSDK zoomSDK = ZoomSDK.getInstance();
        ZoomSDK.getInstance().getMeetingSettingsHelper().enableShowMyMeetingElapseTime(true);
        ZoomSDK.getInstance().getMeetingSettingsHelper().setCustomizedNotificationData(data, handle);

        MeetingService meetingService = zoomSDK.getMeetingService();
        meetingStatusChannel.setStreamHandler(new StatusStreamHandler(meetingService));
        result.success(response);
      }

      @Override
      public void onZoomAuthIdentityExpired() { }
    };
    zoomSDK.initialize(context, listener, initParams);
  }

  private void switchZoomOEM(final MethodCall methodCall, final MethodChannel.Result result) {
    Map<String, String> options = methodCall.arguments();

    boolean success = ZoomSDK.getInstance().switchDomain(options.get("domain"), true);
    Log.d("Zoom Flutter", "switchDomain:" + success);
    Log.d("Zoom Flutter", options.get("domain"));
    if (success) {
      ZoomSDKInitParams initParams = new ZoomSDKInitParams();
      initParams.appKey = options.get("appKey");
      initParams.appSecret = options.get("appSecret");
      initParams.enableLog = true;
      initParams.logSize = 50;
      initParams.domain = options.get("domain");
      initParams.videoRawDataMemoryMode = ZoomSDKRawDataMemoryMode.ZoomSDKRawDataMemoryModeStack;

      ZoomSDK.getInstance().initialize(context, new ZoomSDKInitializeListener() {
        @Override
        public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
          Log.d("Zoom Flutter", "onZoomSDKInitializeResult:" + errorCode + ":" + internalErrorCode);
          result.success(Arrays.asList(errorCode, internalErrorCode));
        }

        @Override
        public void onZoomAuthIdentityExpired() {
          Log.d("Zoom Flutter", "onZoomAuthIdentityExpired:");
        }
      }, initParams);
    }
  }

  //Perform start meeting function with logging in to the zoom account
  private void login(final MethodCall methodCall, final MethodChannel.Result result){
    Map<String, String> options = methodCall.arguments();

    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if(!zoomSDK.isInitialized()) {
      System.out.println("Not initialized!!!!!!");
      result.success(Arrays.asList("SDK ERROR", "001"));
      return;
    }

    if(zoomSDK.isLoggedIn()){
      startMeeting(methodCall, result);
    }

    ZoomSDKAuthenticationListener authenticationListener = new ZoomSDKAuthenticationListener() {

      @Override
      public void onZoomSDKLoginResult(long results) {
        //Log.d("Zoom Flutter", String.format("[onLoginError] : %s", results));
        if (results == ZoomAuthenticationError.ZOOM_AUTH_ERROR_SUCCESS) {
          //Once we verify that the request was successful, we may start the meeting
          startMeeting(methodCall, result);
        }else{
          result.success(Arrays.asList("LOGIN ERROR", String.valueOf(results)));
        }
      }

      @Override
      public void onZoomSDKLogoutResult(long l) {

      }

      @Override
      public void onZoomIdentityExpired() {

      }

      @Override
      public void onZoomAuthIdentityExpired() {

      }

      @Override
      public void onNotificationServiceStatus(SDKNotificationServiceStatus status) {

      }

    };
//    if(!zoomSDK.isLoggedIn()){
//      zoomSDK.loginWithZoom(options.get("userId"), options.get("userPassword"));
//      zoomSDK.addAuthenticationListener(authenticationListener);
//    }
  }
  private Map<String, String> _joinOptions;
  //Join Meeting with passed Meeting ID and Passcode
  private void joinMeeting(MethodCall methodCall, MethodChannel.Result result) {

    Map<String, String> options = methodCall.arguments();
    _joinOptions = options;
    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if(!zoomSDK.isInitialized()) {
      System.out.println("Not initialized!!!!!!");
      result.success(false);
      return;
    }

    zoomSDK.getInMeetingService().setPlayChimeOnOff(true);
    zoomSDK.getZoomUIService().hideMeetingInviteUrl(true);
//    zoomSDK.getInMeetingService().allowParticipantsToRename(false);  // 不允许参会者修改名称
    zoomSDK.getMeetingSettingsHelper().disableShowMeetingNotification(true);
    zoomSDK.getMeetingSettingsHelper().disableShowVideoPreviewWhenJoinMeeting(true);
    zoomSDK.getMeetingSettingsHelper().setGalleryVideoViewDisabled(true);
    zoomSDK.getMeetingSettingsHelper().setTurnOffMyVideoWhenJoinMeeting(true);
    zoomSDK.getMeetingSettingsHelper().setAutoConnectVoIPWhenJoinMeeting(true);
    zoomSDK.getMeetingSettingsHelper().setMuteMyMicrophoneWhenJoinMeeting(true);
    zoomSDK.getMeetingSettingsHelper().disableCopyMeetingUrl(true);

    MeetingService meetingService = zoomSDK.getMeetingService();

    if (mInMeetingServiceListener == null) {
      Log.d("Zoom plugin", "onJoinWebinarNeedUserNameAndEmail: init" );
      mInMeetingServiceListener = new SimpleInMeetingListener() {
        @Override
        public void onJoinWebinarNeedUserNameAndEmail(InMeetingEventHandler inMeetingEventHandler) {
          String name = _joinOptions.get("userId");
          if (name == null) {
            name = "未知";
          }
          String email = _joinOptions.get("email");
          if (email == null) {
            email = "unknown@unknown.com";
          }
          Log.d("Zoom plugin", "onJoinWebinarNeedUserNameAndEmail: " + email);
          inMeetingEventHandler.setRegisterWebinarInfo(name, email, false);
        }
      };
      ZoomSDK.getInstance().getInMeetingService().addListener(mInMeetingServiceListener);
    }

    JoinMeetingOptions opts = new JoinMeetingOptions();
    opts.custom_meeting_id = options.get("meetingTitle");
    opts.customer_key = null;
    opts.no_webinar_register_dialog = true;
    opts.invite_options = InviteOptions.INVITE_DISABLE_ALL;
    opts.meeting_views_options =
            MeetingViewsOptions.NO_TEXT_PASSWORD | MeetingViewsOptions.NO_TEXT_MEETING_ID;
    opts.no_titlebar = false;
    opts.no_bottom_toolbar = false;
    opts.no_chat_msg_toast = false;
    opts.no_dial_in_via_phone = true;
    opts.no_dial_out_to_phone = true;
    opts.no_disconnect_audio = false;
    opts.no_driving_mode = false;
    opts.no_invite = true;
    opts.no_meeting_end_message = false;
    opts.no_meeting_error_message = false;
    opts.no_record = true;
    opts.no_share = false;
    opts.no_unmute_confirm_dialog = false;
    opts.no_video = false;
    opts.no_webinar_register_dialog = true;
    JoinMeetingParams params = new JoinMeetingParams();

    params.displayName = options.get("userId");
    params.meetingNo = options.get("meetingId");
    params.password = options.get("meetingPassword");

    int meetingError = meetingService.joinMeetingWithParams(context, params, opts);
    // 99: MEETING_ERROR_INVALID_ARGUMENTS
    result.success(meetingError);
  }
  // Basic Start Meeting Function called on startMeeting triggered via login function
  private void startMeeting(MethodCall methodCall, MethodChannel.Result result) {

    Map<String, String> options = methodCall.arguments();

    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if(!zoomSDK.isInitialized()) {
      System.out.println("Not initialized!!!!!!");
      result.success(Arrays.asList("SDK ERROR", "001"));
      return;
    }

    if(zoomSDK.isLoggedIn()){
      MeetingService meetingService = zoomSDK.getMeetingService();
      StartMeetingOptions opts= new StartMeetingOptions();
      opts.no_invite = parseBoolean(options, "disableInvite");
      opts.no_share = parseBoolean(options, "disableShare");
      opts.no_driving_mode = parseBoolean(options, "disableDrive");
      opts.no_dial_in_via_phone = parseBoolean(options, "disableDialIn");
      opts.no_disconnect_audio = parseBoolean(options, "noDisconnectAudio");
      opts.no_audio = parseBoolean(options, "noAudio");
      opts.no_titlebar = parseBoolean(options, "disableTitlebar");
      boolean view_options = parseBoolean(options, "viewOptions");
      if(view_options){
        opts.meeting_views_options = MeetingViewsOptions.NO_TEXT_MEETING_ID + MeetingViewsOptions.NO_TEXT_PASSWORD;
      }

      meetingService.startInstantMeeting(context, opts);
      inMeetingService = zoomSDK.getInMeetingService();
      result.success(Arrays.asList("MEETING SUCCESS", "200"));
    }else{
      System.out.println("Not LoggedIn!!!!!!");
      result.success(Arrays.asList("LOGIN REQUIRED", "001"));
      return;
    }
  }

  //Perform start meeting function with logging in to the zoom account (Only for passed meeting id)
  private void startMeetingNormal(final MethodCall methodCall, final MethodChannel.Result result) {

    Map<String, String> options = methodCall.arguments();

    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if(!zoomSDK.isInitialized()) {
      System.out.println("Not initialized!!!!!!");
      result.success(Arrays.asList("SDK ERROR", "001"));
      return;
    }

    if(zoomSDK.isLoggedIn()){
      startMeetingNormalInternal(methodCall, result);
    }

    ZoomSDKAuthenticationListener authenticationListener = new ZoomSDKAuthenticationListener() {

      @Override
      public void onZoomSDKLoginResult(long results) {
        //Log.d("Zoom Flutter", String.format("[onLoginError] : %s", results));
        if (results == ZoomAuthenticationError.ZOOM_AUTH_ERROR_SUCCESS) {
          //Once we verify that the request was successful, we may start the meeting
          startMeetingNormalInternal(methodCall, result);
        }else{
          result.success(Arrays.asList("LOGIN ERROR", String.valueOf(results)));
        }
      }

      @Override
      public void onZoomSDKLogoutResult(long l) {

      }

      @Override
      public void onZoomIdentityExpired() {

      }

      @Override
      public void onZoomAuthIdentityExpired() {

      }

      @Override
      public void onNotificationServiceStatus(SDKNotificationServiceStatus status) {
      }
    };

//    if(!zoomSDK.isLoggedIn()){
//      zoomSDK.loginWithZoom(options.get("userId"), options.get("userPassword"));
//      zoomSDK.addAuthenticationListener(authenticationListener);
//    }
  }

  // Meeting ID passed Start Meeting Function called on startMeetingNormal triggered via startMeetingNormal function
  private void startMeetingNormalInternal(MethodCall methodCall, MethodChannel.Result result) {
    Map<String, String> options = methodCall.arguments();

    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if(!zoomSDK.isInitialized()) {
      System.out.println("Not initialized!!!!!!");
      result.success(Arrays.asList("SDK ERROR", "001"));
      return;
    }

    if(zoomSDK.isLoggedIn()) {
      MeetingService meetingService = zoomSDK.getMeetingService();
      StartMeetingOptions opts = new StartMeetingOptions();
      opts.no_invite = parseBoolean(options, "disableInvite");
      opts.no_share = parseBoolean(options, "disableShare");
      opts.no_driving_mode = parseBoolean(options, "disableDrive");
      opts.no_dial_in_via_phone = parseBoolean(options, "disableDialIn");
      opts.no_disconnect_audio = parseBoolean(options, "noDisconnectAudio");
      opts.no_audio = parseBoolean(options, "noAudio");
      opts.no_titlebar = parseBoolean(options, "disableTitlebar");
      boolean view_options = parseBoolean(options, "viewOptions");
      if (view_options) {
        opts.meeting_views_options = MeetingViewsOptions.NO_TEXT_MEETING_ID + MeetingViewsOptions.NO_TEXT_PASSWORD;
      }

      StartMeetingParams4NormalUser params = new StartMeetingParams4NormalUser();
      params.meetingNo = options.get("meetingId");

      meetingService.startMeetingWithParams(context, params, opts);
      inMeetingService = zoomSDK.getInMeetingService();
      result.success(Arrays.asList("MEETING SUCCESS", "200"));
    }
  }

  //Helper Function for parsing string to boolean value
  private boolean parseBoolean(Map<String, String> options, String property) {
    return options.get(property) != null && Boolean.parseBoolean(options.get(property));
  }

  //Get Meeting Details Programmatically after Starting the Meeting
  private void meetingDetails(MethodChannel.Result result)  {
    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if(!zoomSDK.isInitialized()) {
      System.out.println("Not initialized!!!!!!");
      result.success(Arrays.asList("MEETING_STATUS_UNKNOWN", "SDK not initialized"));
      return;
    }
    MeetingService meetingService = zoomSDK.getMeetingService();

    if(meetingService == null) {
      result.success(Arrays.asList("MEETING_STATUS_UNKNOWN", "No status available"));
      return;
    }
    MeetingStatus status = meetingService.getMeetingStatus();

    result.success(status != null ? Arrays.asList(inMeetingService.getCurrentMeetingNumber(), inMeetingService.getMeetingPassword()) :  Arrays.asList("MEETING_STATUS_UNKNOWN", "No status available"));
  }

  //Listen to meeting status on joinning and starting the mmeting
  private void meetingStatus(MethodChannel.Result result) {

    ZoomSDK zoomSDK = ZoomSDK.getInstance();

    if(!zoomSDK.isInitialized()) {
      System.out.println("Not initialized!!!!!!");
      result.success(Arrays.asList("MEETING_STATUS_UNKNOWN", "SDK not initialized"));
      return;
    }
    MeetingService meetingService = zoomSDK.getMeetingService();

    if(meetingService == null) {
      result.success(Arrays.asList("MEETING_STATUS_UNKNOWN", "No status available"));
      return;
    }

    MeetingStatus status = meetingService.getMeetingStatus();
    result.success(status != null ? Arrays.asList(status.name(), "") :  Arrays.asList("MEETING_STATUS_UNKNOWN", "No status available"));
  }

  public void logout() {
    ZoomSDK zoomSDK = ZoomSDK.getInstance();
    zoomSDK.logoutZoom();
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {

  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {

  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

  }

  @Override
  public void onDetachedFromActivity() {

  }
}

class SimpleInMeetingListener implements InMeetingServiceListener {

  @Override
  public void onMeetingNeedPasswordOrDisplayName(boolean b, boolean b1, InMeetingEventHandler inMeetingEventHandler) {

  }

  @Override
  public void onWebinarNeedRegister(String s) {

  }

  @Override
  public void onJoinWebinarNeedUserNameAndEmail(InMeetingEventHandler inMeetingEventHandler) {

  }

  @Override
  public void onMeetingNeedCloseOtherMeeting(InMeetingEventHandler inMeetingEventHandler) {

  }

  @Override
  public void onMeetingFail(int i, int i1) {

  }

  @Override
  public void onMeetingLeaveComplete(long l) {

  }

  @Override
  public void onMeetingUserJoin(List<Long> list) {

  }

  @Override
  public void onMeetingUserLeave(List<Long> list) {

  }

  @Override
  public void onMeetingUserUpdated(long l) {

  }

  @Override
  public void onMeetingHostChanged(long l) {

  }

  @Override
  public void onMeetingCoHostChanged(long l) {

  }

  @Override
  public void onMeetingCoHostChange(long l, boolean b) {

  }

  @Override
  public void onActiveVideoUserChanged(long l) {

  }

  @Override
  public void onActiveSpeakerVideoUserChanged(long l) {

  }

  @Override
  public void onHostVideoOrderUpdated(List<Long> list) {

  }

  @Override
  public void onFollowHostVideoOrderChanged(boolean b) {

  }

  @Override
  public void onSpotlightVideoChanged(boolean b) {

  }

  @Override
  public void onSpotlightVideoChanged(List<Long> list) {

  }

  @Override
  public void onUserVideoStatusChanged(long l, VideoStatus videoStatus) {

  }

  @Override
  public void onUserNetworkQualityChanged(long l) {

  }

  @Override
  public void onSinkMeetingVideoQualityChanged(VideoQuality videoQuality, long l) {

  }

  @Override
  public void onMicrophoneStatusError(InMeetingAudioController.MobileRTCMicrophoneError mobileRTCMicrophoneError) {

  }

  @Override
  public void onUserAudioStatusChanged(long l, AudioStatus audioStatus) {

  }

  @Override
  public void onHostAskUnMute(long l) {

  }

  @Override
  public void onHostAskStartVideo(long l) {

  }

  @Override
  public void onUserAudioTypeChanged(long l) {

  }

  @Override
  public void onMyAudioSourceTypeChanged(int i) {

  }

  @Override
  public void onLowOrRaiseHandStatusChanged(long l, boolean b) {

  }

  @Override
  public void onChatMessageReceived(InMeetingChatMessage inMeetingChatMessage) {

  }

  @Override
  public void onChatMsgDeleteNotification(String s, ChatMessageDeleteType chatMessageDeleteType) {

  }

  @Override
  public void onShareMeetingChatStatusChanged(boolean b) {

  }

  @Override
  public void onSilentModeChanged(boolean b) {

  }

  @Override
  public void onFreeMeetingReminder(boolean b, boolean b1, boolean b2) {

  }

  @Override
  public void onMeetingActiveVideo(long l) {

  }

  @Override
  public void onSinkAttendeeChatPriviledgeChanged(int i) {

  }

  @Override
  public void onSinkAllowAttendeeChatNotification(int i) {

  }

  @Override
  public void onSinkPanelistChatPrivilegeChanged(InMeetingChatController.MobileRTCWebinarPanelistChatPrivilege mobileRTCWebinarPanelistChatPrivilege) {

  }

  @Override
  public void onUserNameChanged(long l, String s) {

  }

  @Override
  public void onUserNamesChanged(List<Long> list) {

  }

  @Override
  public void onFreeMeetingNeedToUpgrade(FreeMeetingNeedUpgradeType freeMeetingNeedUpgradeType, String s) {

  }

  @Override
  public void onFreeMeetingUpgradeToGiftFreeTrialStart() {

  }

  @Override
  public void onFreeMeetingUpgradeToGiftFreeTrialStop() {

  }

  @Override
  public void onFreeMeetingUpgradeToProMeeting() {

  }

  @Override
  public void onClosedCaptionReceived(String s, long l) {

  }

  @Override
  public void onRecordingStatus(RecordingStatus recordingStatus) {

  }

  @Override
  public void onLocalRecordingStatus(long l, RecordingStatus recordingStatus) {

  }

  @Override
  public void onInvalidReclaimHostkey() {

  }

  @Override
  public void onPermissionRequested(String[] strings) {

  }

  @Override
  public void onAllHandsLowered() {

  }

  @Override
  public void onLocalVideoOrderUpdated(List<Long> list) {

  }

  @Override
  public void onLocalRecordingPrivilegeRequested(IRequestLocalRecordingPrivilegeHandler handler) {

  }

}

