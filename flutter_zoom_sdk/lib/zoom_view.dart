import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter_zoom_sdk/zoom_platform_view.dart';

class ZoomView extends ZoomPlatform {
  final MethodChannel channel = const MethodChannel('com.evilratt/zoom_sdk');

  /// The event channel used to interact with the native platform.
  final EventChannel eventChannel =
      const EventChannel('com.evilratt/zoom_sdk_event_stream');

  /// The event channel used to interact with the native platform init function
  @override
  Future<List> initZoom(ZoomOptions options) async {
    Map<String, String?> optionMap = <String, String?>{};

    if (options.appKey != null) {
      optionMap.putIfAbsent("appKey", () => options.appKey!);
    }
    if (options.appSecret != null) {
      optionMap.putIfAbsent("appSecret", () => options.appSecret!);
    }

    optionMap.putIfAbsent("domain", () => options.domain);
    return channel
        .invokeMethod<List>('init', optionMap)
        .then<List>((List? value) => value ?? List.empty());
  }

  @override
  Future<List> switchZoomOEM(ZoomOptions options) async {
    Map<String, String?> optionMap = <String, String?>{};

    if (options.appKey != null) {
      optionMap.putIfAbsent("appKey", () => options.appKey!);
    }
    if (options.appSecret != null) {
      optionMap.putIfAbsent("appSecret", () => options.appSecret!);
    }

    optionMap.putIfAbsent("domain", () => options.domain);
    return channel
        .invokeMethod<List>('switchZoomOEM', optionMap)
        .then<List>((List? value) => value ?? List.empty());
  }



  /// The event channel used to interact with the native platform joinMeeting function
  @override
  Future<int> joinMeeting(ZoomMeetingOptions options) async {
    return channel
        .invokeMethod<int>('join', options.toJson())
        .then<int>((int? value) => value ?? -1);
  }

  /// The event channel used to interact with the native platform meetingStatus function
  @override
  Future<List> meetingStatus(String meetingId) async {
    var optionMap = <String, String>{};
    optionMap.putIfAbsent("meetingId", () => meetingId);

    return channel
        .invokeMethod<List>('meeting_status', optionMap)
        .then<List>((List? value) => value ?? List.empty());
  }

  /// The event channel used to interact with the native platform onMeetingStatus(iOS & Android) function
  @override
  Stream<dynamic> onMeetingStatus() {
    return eventChannel.receiveBroadcastStream();
  }

  /// The event channel used to interact with the native platform meetinDetails(iOS & Android) function
  @override
  Future<List> meetinDetails() async {
    return channel
        .invokeMethod<List>('meeting_details')
        .then<List>((List? value) => value ?? List.empty());
  }
}
