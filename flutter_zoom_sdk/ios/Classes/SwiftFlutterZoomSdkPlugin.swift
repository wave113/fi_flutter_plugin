import Flutter
import UIKit
import MobileRTC
import ReplayKit

public class SwiftFlutterZoomSdkPlugin: NSObject, FlutterPlugin,FlutterStreamHandler , MobileRTCMeetingServiceDelegate {

  var authenticationDelegate: AuthenticationDelegate
  var eventSink: FlutterEventSink?
    var userId: String?
    var meetingInfo: Dictionary<String, String?>?

  public static func register(with registrar: FlutterPluginRegistrar) {
    let messenger = registrar.messenger()
    let channel = FlutterMethodChannel(name: "com.evilratt/zoom_sdk", binaryMessenger: messenger)
    let instance = SwiftFlutterZoomSdkPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)

    let eventChannel = FlutterEventChannel(name: "com.evilratt/zoom_sdk_event_stream", binaryMessenger: messenger)
    eventChannel.setStreamHandler(instance)
      instance.observeApplicationLifecycle()
  }

  override init(){
      authenticationDelegate = AuthenticationDelegate()
  }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    func observeApplicationLifecycle() {
        NotificationCenter.default.addObserver(self, selector: #selector(applicationWillResignActive), name: UIApplication.willResignActiveNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(applicationDidEnterBackground), name: UIApplication.didEnterBackgroundNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(applicationDidBecomeActive), name: UIApplication.didBecomeActiveNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(applicationWillTerminate), name: UIApplication.willTerminateNotification, object: nil)
    }
    
    @objc func applicationWillResignActive() {
        MobileRTC.shared().appWillResignActive()
    }
    
    @objc func applicationDidEnterBackground() {
        MobileRTC.shared().appDidEnterBackgroud()
    }
    
    @objc func applicationDidBecomeActive() {
        MobileRTC.shared().appDidBecomeActive()
    }
    
    @objc func applicationWillTerminate() {
        MobileRTC.shared().appWillTerminate()
    }
    
    

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
          switch call.method {
          case "init":
              self.initZoom(call: call, result: result)
          case "switchZoomOEM":
              self.switchZoomOEM(call: call, result: result)
          case "join":
              self.joinMeeting(call: call, result: result)
          case "meeting_status":
              self.meetingStatus(call: call, result: result)
          case "meeting_details":
              self.meetingDetails(call: call, result: result)
          default:
              result(FlutterMethodNotImplemented)
          }
  }

  public func onMethodCall(call: FlutterMethodCall, result: @escaping FlutterResult) {

          switch call.method {
          case "init":
              self.initZoom(call: call, result: result)
          case "switchZoomOEM":
              self.switchZoomOEM(call: call, result: result)
              break
          case "join":
              self.joinMeeting(call: call, result: result)
          case "meeting_status":
              self.meetingStatus(call: call, result: result)
          case "meeting_details":
              self.meetingDetails(call: call, result: result)
          default:
              result(FlutterMethodNotImplemented)
          }
      }

        //Initializing the Zoom SDK for iOS
        public func initZoom(call: FlutterMethodCall, result: @escaping FlutterResult)  {

            let pluginBundle = Bundle(for: type(of: self))
            let pluginBundlePath = pluginBundle.bundlePath
            let arguments = call.arguments as! Dictionary<String, String>

            let context = MobileRTCSDKInitContext()
            context.domain = arguments["domain"]!
            context.enableLog = true
            context.bundleResPath = pluginBundlePath
            context.locale = .default
            MobileRTC.shared().initialize(context)

            let auth = MobileRTC.shared().getAuthService()
            auth?.delegate = self.authenticationDelegate.onAuth(result)
            if let appKey = arguments["appKey"] {
                auth?.clientKey = appKey
            }
            if let appSecret = arguments["appSecret"] {
                auth?.clientSecret = appSecret
            }
            
            auth?.sdkAuth()
        }
    
    
    public func switchZoomOEM(call: FlutterMethodCall, result: @escaping FlutterResult)  {
        let arguments = call.arguments as! Dictionary<String, String>

        let switchResult = MobileRTC.shared().switchDomain(arguments["domain"]!, force: true)
        if switchResult == false {
            result([-100, 0])
            return
        }
        let auth = MobileRTC.shared().getAuthService()
        auth?.delegate = self.authenticationDelegate.onAuth(result)
        if let appKey = arguments["appKey"] {
            auth?.clientKey = appKey
        }
        if let appSecret = arguments["appSecret"] {
            auth?.clientSecret = appSecret
        }
        auth?.sdkAuth()
    }

        //Listen to meeting status on joinning and starting the mmeting
        public func meetingStatus(call: FlutterMethodCall, result: FlutterResult) {

            let meetingService = MobileRTC.shared().getMeetingService()
            if meetingService != nil {
                let meetingState = meetingService?.getMeetingState()
                result(getStateMessage(meetingState))
            } else {
                result(["MEETING_STATUS_UNKNOWN", ""])
            }
       }
    
        //Get Meeting Details Programmatically after Starting the Meeting
        public func meetingDetails(call: FlutterMethodCall, result: FlutterResult) {

            let meetingService = MobileRTC.shared().getMeetingService()
            if meetingService != nil {
                let meetingPassword = MobileRTCInviteHelper.sharedInstance().rawMeetingPassword
                let meetingNumber = MobileRTCInviteHelper.sharedInstance().ongoingMeetingNumber
                
                result([meetingNumber, meetingPassword])
                
            } else {
                result(["MEETING_STATUS_UNKNOWN", "No status available"])
            }
        }

        //Join Meeting with passed Meeting ID and Passcode
        public func joinMeeting(call: FlutterMethodCall, result: FlutterResult) {
            
            let meetingService = MobileRTC.shared().getMeetingService()
            let meetingSettings = MobileRTC.shared().getMeetingSettings()
            if (meetingService != nil) {
//                meetingService?.allowParticipants(toRename: false) // 不允许自己改名
                
                let arguments = call.arguments as! Dictionary<String, String?>
                meetingInfo = arguments

//                meetingSettings?.meetingVideoHidden = true
//                meetingSettings?.meetingAudioHidden = true // 可以直接连接语音的话，就可以直接隐藏掉该按钮
                
//                meetingSettings?.meetingTitleHidden = true // 可以达到隐藏会议ID目的， 实际并不行
                meetingSettings?.disableGalleryView(true)     // 视图 画廊模式
                meetingSettings?.disableCopyMeetingUrl(true)
                meetingSettings?.meetingShareHidden = false
                meetingSettings?.meetingInviteHidden = true
                meetingSettings?.meetingInviteUrlHidden = true
//                meetingSettings?.meetingMoreHidden = true
                meetingSettings?.meetingPasswordHidden = true
//                meetingSettings?.meetingParticipantHidden = true // 是否要隐藏参会列表
                meetingSettings?.disableCall(in: true)
                meetingSettings?.setMuteAudioWhenJoinMeeting(true) // 加入会议时自我静音
                meetingSettings?.setMuteVideoWhenJoinMeeting(true) // 加入会议时不打开摄像头
                meetingSettings?.setAutoConnectInternetAudio(true) // 加入会议时自动收听他人声音
                meetingSettings?.disableMinimizeMeeting(false)
                meetingSettings?.disableDriveMode(false) // 是否禁用驾驶模式
//                meetingSettings?.hideReactions(onMeetingUI: true)
//                meetingSettings?.meetingChatHidden = true  // 需要配置
                meetingSettings?.recordButtonHidden = true
//                 meetingSettings?.thumbnailInShare = true // 不能打开该开关，都在SDK会Crash
//                meetingSettings?.enableKubi = false
//                meetingSettings?.claimHostWithHostKeyHidden = true
//                meetingSettings?.closeCaptionHidden = false
//                meetingSettings?.qaButtonHidden = true
//                meetingSettings?.changeToAttendeeHidden = true
                
                let joinMeetingParameters = MobileRTCMeetingJoinParam()
//                joinMeetingParameters.noAudio = true
//                joinMeetingParameters.noVideo = true
                joinMeetingParameters.userName = arguments["userId"]!!
                userId = joinMeetingParameters.userName
                joinMeetingParameters.meetingNumber = arguments["meetingId"]!!
                
                let hasPassword = arguments["meetingPassword"]! != nil
                if hasPassword {
                    joinMeetingParameters.password = arguments["meetingPassword"]!!
                }
                
                meetingService?.customizeMeetingTitle(arguments["meetingTitle"] ?? "直播")
                //Joining the meeting and storing the response
                let response = meetingService?.joinMeeting(with: joinMeetingParameters)

                if let response = response {
                    debugPrint("Got response from join: \(response.rawValue)")
// todo: 安卓的也需要修改
                    if (response == .inAnotherMeeting) {
                        debugPrint("zoom: 遇到了 inAnotherMeeting错误，尝试修复")
                        meetingService?.leaveMeeting(with: .leave)
                        let response2 = meetingService?.joinMeeting(with: joinMeetingParameters)
                        debugPrint("zoom: 遇到了 inAnotherMeeting错误，修复结果：\(response2?.rawValue == 0)")
                        if let response3 = response2 {
                            result(response3.rawValue)
                            return
                        }
                    } else {
                        result(response.rawValue)
                        return
                    }
                }
            }
            result(-1)
        }

        //Helper Function for parsing string to boolean value
        private func parseBoolean(data: String?, defaultValue: Bool) -> Bool {
            var result: Bool

            if let unwrappeData = data {
                result = NSString(string: unwrappeData).boolValue
            } else {
               result = defaultValue
            }
            return result
        }
    
        //Helper Function for parsing string to int value
        private func parseInt(data: String?, defaultValue: Int) -> Int {
            var result: Int

            if let unwrappeData = data {
                result = NSString(string: unwrappeData).integerValue
            } else {
               result = defaultValue
            }
            return result
        }
    
    public func onClickedShareButton(_ parentVC: UIViewController, addShareActionItem array: NSMutableArray) -> Bool {
        return false
    }
    
    public func showAlert(messsage: String) {
        DispatchQueue.main.async {
            let alert = UIAlertView.init(title: "提示", message: messsage, delegate: nil, cancelButtonTitle: "确定")
            alert.show()
        }
    }


        public func onMeetingError(_ error: MobileRTCMeetError, message: String?) {
            debugPrint("onMeetingError \(error.rawValue)")
        }

        public func getMeetErrorMessage(_ errorCode: MobileRTCMeetError) -> String {
            debugPrint("getMeetErrorMessage \(errorCode.rawValue)")
            let message = ""
            return message
        }

        public func onMeetingStateChange(_ state: MobileRTCMeetingState) {

            guard let eventSink = eventSink else {
                return
            }
            eventSink(getStateMessage(state))
//            if (state == .inMeeting) {
//                let meetingService = MobileRTC.shared().getMeetingService()
//                if meetingService != nil, let meetingView = meetingService?.meetingView() {
//                    findAndHideUserNameView(meetingView)
//                }
//            }
        }
        // 隐藏参会用户信息
    private func findAndHideUserNameView(_ view: UIView) {
        let subviews = view.subviews
        if subviews.count > 0 {
            subviews.forEach { view in
                if view.isKind(of: UILabel.self), let label: UILabel = view as? UILabel, label.text == userId {
                    if let vc = findViewController(for: view) {
                        let vcClassName = type(of: vc).description()
                        if vcClassName.contains("ZMThumbnailViewController") {
                            debugPrint("zoom: hide vc: \(vc) \(String(describing: vc.view))  super: \(String(describing: vc.view.superview))")
                            vc.view.isHidden = true
                            vc.view.removeFromSuperview()
                            vc.removeFromParent()
                            debugPrint("zoom: hide vc 2 : \(String(describing: vc.parent)) \(String(describing: vc.view)) super: \(vc.view.superview)")
                        }
                    }
                    return
                }
                findAndHideUserNameView(view)
            }
        }
    }
    
    private func findViewController(for view: UIView) -> UIViewController? {
        var next = view.next
        repeat {
            if next?.isKind(of: UIViewController.self) ?? false {
                return next as? UIViewController
            }
            next = next?.next
        } while(next != nil)
        return nil
    }
    
    
        //Listen to initializing sdk events
        public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
            self.eventSink = events

            let meetingService = MobileRTC.shared().getMeetingService()
            if meetingService == nil {
                return FlutterError(code: "Zoom SDK error", message: "ZoomSDK is not initialized", details: nil)
            }
            meetingService?.delegate = self

            return nil
        }

        public func onCancel(withArguments arguments: Any?) -> FlutterError? {
            eventSink = nil
            return nil
        }
    
        //Get Meeting Status message with proper codes
        private func getStateMessage(_ state: MobileRTCMeetingState?) -> [String] {

            var message: [String]
                switch state {
                case  .idle:
                    message = ["MEETING_STATUS_IDLE", "No meeting is running"]
                    break
                case .connecting:
                    message = ["MEETING_STATUS_CONNECTING", "Connect to the meeting server"]
                    break
                case .inMeeting:
                    message = ["MEETING_STATUS_INMEETING", "Meeting is ready and in process"]
                    break
                case .webinarPromote:
                    message = ["MEETING_STATUS_WEBINAR_PROMOTE", "Upgrade the attendees to panelist in webinar"]
                    break
                case .webinarDePromote:
                    message = ["MEETING_STATUS_WEBINAR_DEPROMOTE", "Demote the attendees from the panelist"]
                    break
                case .disconnecting:
                    message = ["MEETING_STATUS_DISCONNECTING", "Disconnect the meeting server, leave meeting status"]
                    break;
                case .ended:
                    message = ["MEETING_STATUS_ENDED", "Meeting ends"]
                    break;
                case .failed:
                    message = ["MEETING_STATUS_FAILED", "Failed to connect the meeting server"]
                    break;
                case .reconnecting:
                    message = ["MEETING_STATUS_RECONNECTING", "Reconnecting meeting server status"]
                    break;
                case .waitingForHost:
                    message = ["MEETING_STATUS_WAITINGFORHOST", "Waiting for the host to start the meeting"]
                    break;
                case .inWaitingRoom:
                    message = ["MEETING_STATUS_IN_WAITING_ROOM", "Participants who join the meeting before the start are in the waiting room"]
                    break;
                default:
                    message = ["MEETING_STATUS_UNKNOWN", "'(state?.rawValue ?? 9999)'"]
                }
            return message
            }
        }

        //Zoom SDK Authentication Listner
        public class AuthenticationDelegate: NSObject, MobileRTCAuthDelegate {

            private var result: FlutterResult?

            //Zoom SDK Authentication Listner - On Auth get result
            public func onAuth(_ result: FlutterResult?) -> AuthenticationDelegate {
                self.result = result
                return self
            }

            //Zoom SDK Authentication Listner - On MobileRTCAuth get result
            public func onMobileRTCAuthReturn(_ returnValue: MobileRTCAuthError) {
                if returnValue == .success {
                    self.result?([0, 0])
                } else {
                    self.result?([returnValue.rawValue, 0])
                }

                self.result = nil
            }
            
            //Zoom SDK Authentication Listner - On onMobileRTCLoginReturn get status
//            public func onMobileRTCLoginReturn(_ returnValue: Int){
//
//            }

            //Zoom SDK Authentication Listner - On onMobileRTCLogoutReturn get message
            public func onMobileRTCLogoutReturn(_ returnValue: Int) {

            }
            
            //Zoom SDK Authentication Listner - On getAuthErrorMessage get message
            public func getAuthErrorMessage(_ errorCode: MobileRTCAuthError) -> String {

                let message = ""

                return message
            }
        }

extension SwiftFlutterZoomSdkPlugin: MobileRTCMeetingShareActionItemDelegate {
    public func onShareItemClicked(_ tag: UInt, completion: @escaping (UIViewController) -> Bool) {
    
    }
    
}


extension SwiftFlutterZoomSdkPlugin: MobileRTCWebinarServiceDelegate {
    public func onSinkQAConnectStarted() {
        
    }

    public func onSinkQAConnected(_ connected: Bool) {
        
    }

    public func onSinkQAOpenQuestionChanged(_ count: Int) {
        
    }

    public func onSinkQAAddQuestion(_ questionID: String, success: Bool) {
        
    }

    public func onSinkQAAddAnswer(_ answerID: String, success: Bool) {
        
    }

    public func onSinkQuestionMarked(asDismissed questionID: String) {
        
    }

    public func onSinkReopenQuestion(_ questionID: String) {
        
    }

    public func onSinkReceiveQuestion(_ questionID: String) {
        
    }

    public func onSinkReceiveAnswer(_ answerID: String) {
        
    }

    public func onSinkUserLivingReply(_ questionID: String) {
        
    }

    public func onSinkUserEndLiving(_ questionID: String) {
        
    }

    public func onSinkVoteupQuestion(_ questionID: String, orderChanged: Bool) {
        
    }

    public func onSinkRevokeVoteupQuestion(_ questionID: String, orderChanged: Bool) {
        
    }

    public func onSinkDeleteQuestion(_ questionIDArray: [Any]) {
        
    }

    public func onSinkDeleteAnswer(_ answerIDArray: [Any]) {
        
    }

    public func onSinkQAAllowAskQuestionAnonymouslyNotification(_ beAllowed: Bool) {
        
    }

    public func onSinkQAAllowAttendeeViewAllQuestionNotification(_ beAllowed: Bool) {
        
    }

    public func onSinkQAAllowAttendeeUpVoteQuestionNotification(_ beAllowed: Bool) {
        
    }

    public func onSinkQAAllowAttendeeAnswerQuestionNotification(_ beAllowed: Bool) {
        
    }

    public func onSinkWebinarNeedRegister(_ registerURL: String) {
        
    }

    public func onSinkJoinWebinarNeedUserNameAndEmail(completion: @escaping (String, String, Bool) -> Bool) {
        let name: String = (meetingInfo?["userId"] ?? "未知") ?? "未知"
        let randomNumber = arc4random() % 10000
        let unkownEmail = String(randomNumber) + "unknown@unknown.com"
        let email: String = (meetingInfo?["email"] ?? unkownEmail) ?? unkownEmail
        _ = completion(name, email, false)
    }

    public func onSinkPanelistCapacityExceed() {
        
    }

    public func onSinkPromptAttendee2PanelistResult(_ errorCode: MobileRTCWebinarPromoteorDepromoteError) {
        
    }

    public func onSinkDePromptPanelist2AttendeeResult(_ errorCode: MobileRTCWebinarPromoteorDepromoteError) {
        
    }

    public func onSinkAllowAttendeeChatNotification(_ currentPrivilege: MobileRTCChatAllowAttendeeChat) {
        
    }

    public func onSinkAttendeePromoteConfirmResult(_ agree: Bool, userId: UInt) {
        
    }

    public func onSinkSelfAllowTalkNotification() {
        
    }

    public func onSinkSelfDisallowTalkNotification() {
        
    }

    
}


