import 'dart:async';
import 'package:flutter/services.dart';

class VapController {
  static const MethodChannel _channel =
      const MethodChannel('flutter_vap_controller');

  /// return: play error:       {"status": "failure", "errorMsg": ""}
  ///         play complete:    {"status": "complete"}
  static Future<Map<dynamic, dynamic>?> playPath(String path) async {
    return _channel.invokeMethod('playPath', {"path": path});
  }

  static Future<Map<dynamic, dynamic>?> playAsset(
    String asset, {
    Map<String, String> imageProperties = const {},
    Map<String, String> textProperties = const {},
  }) {
    return _channel.invokeMethod('playAsset', {
      "asset": asset,
      "imageProperties": imageProperties,
      "textProperties": textProperties,
    });
  }

  static stop() {
    _channel.invokeMethod('stop');
  }
}
