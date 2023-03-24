/// Basic Zoom Options required for plugin (WEB, iOS, Android)
class ZoomOptions {
  String? domain;

  /// Domain For Zoom Web
  String? appKey;

  /// --JWT key for web / SDK key for iOS / Android
  String? appSecret;

  /// --JWT secret for web / SDK secret for iOS / Android
  String? language;

  /// --Language for web
  bool? showMeetingHeader;

  /// --Meeting Header for web
  bool? disableInvite;

  /// --Disable Invite Option for web
  bool? disableCallOut;

  /// --Disable CallOut Option for web
  bool? disableRecord;

  /// --Disable Record Option for web
  bool? disableJoinAudio;

  /// --Disable Join Audio for web
  bool? audioPanelAlwaysOpen;

  /// -- Allow Pannel Always Open for web
  bool? isSupportAV;

  /// --AV Support for web
  bool? isSupportChat;

  /// --Chat Suppport for web
  bool? isSupportQA;

  /// --QA Support for web
  bool? isSupportCC;

  /// --CC Support for web
  bool? isSupportPolling;

  /// --Polling Support for web
  bool? isSupportBreakout;

  /// -- Breakout Support for web
  bool? screenShare;

  /// --Screen Sharing Option for web
  String? rwcBackup;

  /// --RWC Backup Option for web
  bool? videoDrag;

  /// -- Drag Video Option for web
  String? sharingMode;

  /// --Sharing Mode for web
  bool? videoHeader;

  /// --Video Header for web
  bool? isLockBottom;

  /// --Lock Bottom Support for web
  bool? isSupportNonverbal;

  /// --Nonverbal Support for web
  bool? isShowJoiningErrorDialog;

  /// --Error Dialog Visibility for web
  bool? disablePreview;

  /// --Disable Preview for web
  bool? disableCORP;

  /// --Disable Crop for web
  String? inviteUrlFormat;

  /// --Invite Url Format for web
  bool? disableVOIP;

  /// --Disable VOIP for web
  bool? disableReport;

  /// --Disable Report for web
  List<String>? meetingInfo;

  /// --Meeting Info for web

  ZoomOptions(
      {required this.domain,
      this.appKey,
      this.appSecret,
      this.language = "zh-cn",
      this.showMeetingHeader = true,
      this.disableInvite = false,
      this.disableCallOut = false,
      this.disableRecord = false,
      this.disableJoinAudio = false,
      this.audioPanelAlwaysOpen = false,
      this.isSupportAV = true,
      this.isSupportChat = true,
      this.isSupportQA = true,
      this.isSupportCC = true,
      this.isSupportPolling = true,
      this.isSupportBreakout = true,
      this.screenShare = true,
      this.rwcBackup = '',
      this.videoDrag = true,
      this.sharingMode = 'both',
      this.videoHeader = true,
      this.isLockBottom = true,
      this.isSupportNonverbal = true,
      this.isShowJoiningErrorDialog = true,
      this.disablePreview = false,
      this.disableCORP = true,
      this.inviteUrlFormat = '',
      this.disableVOIP = false,
      this.disableReport = false,
      this.meetingInfo = const [
        'topic',
        'host',
        'mn',
        'pwd',
        'telPwd',
        'invite',
        'participant',
        'dc',
        'enctype',
        'report'
      ]});
}

/// Basic Zoom Meeting Options required for plugin (WEB, iOS, Android)
class ZoomMeetingOptions {
  String email;
  String meetingTitle;
  String userId; // 用户名
  String meetingId;
  String meetingPassword;

  ZoomMeetingOptions({
    required this.userId,
    required this.email,
    required this.meetingId,
    required this.meetingPassword,
    required this.meetingTitle,
  });

  Map<String, String> toJson() {
    return {"userId": userId,
      "email": email,
      "meetingId": meetingId,
      "meetingPassword": meetingPassword,
      "meetingTitle": meetingTitle
    };
  }

}
