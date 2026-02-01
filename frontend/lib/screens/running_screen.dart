import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:geolocator/geolocator.dart';
import 'package:runtogether_team04/constants.dart';
import 'package:stop_watch_timer/stop_watch_timer.dart';
import 'package:health/health.dart';
import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_background_service/flutter_background_service.dart';
import 'package:flutter/foundation.dart'; // kIsWeb 확인용
import 'package:flutter_tts/flutter_tts.dart'; // TTS 패키지

class RunningScreen extends StatefulWidget {
  final int groupId;
  final int courseId;

  const RunningScreen({
    super.key,
    required this.groupId,
    required this.courseId,
  });

  @override
  State<RunningScreen> createState() => _RunningScreenState();
}

class _RunningScreenState extends State<RunningScreen> {
  // 구글맵 컨트롤러
  final Completer<GoogleMapController> _controller = Completer();

  // 위치 데이터
  Position? _currentPosition;
  final List<LatLng> _myRouteCoords = []; // 내가 뛴 경로 (저장용)

  // ★ 네비게이션용 코스 전체 경로 데이터
  List<LatLng> _globalCoursePoints = [];

  // 지도 요소
  final Set<Polyline> _polylines = {};
  final Set<Marker> _markers = {};

  // 러닝 데이터
  final StopWatchTimer _stopWatchTimer = StopWatchTimer(mode: StopWatchMode.countUp);
  double _totalDistance = 0.0;
  double _calories = 0.0;
  String _pace = "0'00''";
  int _heartRate = 0;
  final Health _health = Health();

  // 상태 관리
  bool _isAiCoachOn = true; // 기본값 true (저장된 설정 불러옴)
  bool _isNaviOn = false;
  bool _isSaving = false;

  // 러닝 시작 상태 관리 & 코스 시작점 저장
  bool _isRunStarted = false;
  LatLng? _courseStartPoint;

  // 백그라운드 데이터 리스너
  StreamSubscription? _serviceSubscription;

  // ★ TTS 관련 변수
  final FlutterTts _flutterTts = FlutterTts();
  DateTime _lastSpeakTime = DateTime.now();

  @override
  void initState() {
    super.initState();

    _checkPermission();         // 위치 권한 및 초기 위치
    _health.configure();
    _fetchCoursePath();         // 코스 경로 로딩
    _startBackgroundService();  // 백그라운드 서비스 시작
    _fetchHealthData();         // 심박수 수집

    // ★ TTS 초기화 및 설정 로드
    _initTts();
    _loadAiCoachSetting();
  }

  @override
  void dispose() {
    _stopWatchTimer.dispose();
    _serviceSubscription?.cancel();
    _flutterTts.stop();
    super.dispose();
  }

  // ------------------------------------------------------------------------
  // ★ TTS 및 설정 관련 함수
  // ------------------------------------------------------------------------
  Future<void> _initTts() async {
    await _flutterTts.setLanguage("ko-KR");
    await _flutterTts.setSpeechRate(0.5);
    await _flutterTts.setPitch(1.0);
  }

  // 환경설정에서 AI 코치 ON/OFF 값 불러오기
  Future<void> _loadAiCoachSetting() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      bool isVoiceOn = prefs.getBool('ai_coach_voice') ?? true;
      setState(() {
        _isAiCoachOn = isVoiceOn;
      });
      print("🎧 AI 코치 설정 로드됨: ${_isAiCoachOn ? 'ON' : 'OFF'}");
    } catch (e) {
      print("설정 로드 실패: $e");
    }
  }

  // 음성 안내 실행 (고정 쿨타임 10초)
  Future<void> _speak(String text) async {
    // 1. 꺼져있으면 안 함
    if (!_isAiCoachOn) return;

    // 2. 쿨타임 10초
    if (DateTime.now().difference(_lastSpeakTime).inSeconds < 10) return;

    _lastSpeakTime = DateTime.now();
    await _flutterTts.speak(text);
  }

  // ------------------------------------------------------------------------
  // 위치 권한 및 초기 위치, 카메라 이동
  // ------------------------------------------------------------------------
  Future<void> _checkPermission() async {
    LocationPermission permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.deniedForever) return;

    try {
      Position position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );
      if (mounted) {
        setState(() => _currentPosition = position);
        final c = await _controller.future;
        c.animateCamera(CameraUpdate.newLatLngZoom(
            LatLng(position.latitude, position.longitude), 16));
      }
    } catch (e) {
      print("초기 위치 로드 실패: $e");
    }
  }

  // ★ [추가] 내 위치로 카메라 이동 함수 (버튼 클릭 시 사용)
  Future<void> _moveToCurrentLocation() async {
    if (_currentPosition == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("현재 위치를 찾을 수 없습니다.")));
      return;
    }

    try {
      final GoogleMapController controller = await _controller.future;
      controller.animateCamera(CameraUpdate.newLatLngZoom(
        LatLng(_currentPosition!.latitude, _currentPosition!.longitude),
        18, // 조금 더 확대해서 보여줌
      ));
    } catch (e) {
      print("카메라 이동 실패: $e");
    }
  }

  // ------------------------------------------------------------------------
  // 팝업 로직 (시작 전/후)
  // ------------------------------------------------------------------------
  void _tryStartRun() {
    if (_currentPosition == null || _courseStartPoint == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("위치 정보를 불러오는 중입니다.")),
      );
      return;
    }

    double dist = Geolocator.distanceBetween(
      _currentPosition!.latitude, _currentPosition!.longitude,
      _courseStartPoint!.latitude, _courseStartPoint!.longitude,
    );

    if (dist <= 100) {
      _startRealRun();
    } else {
      showDialog(
        context: context,
        barrierDismissible: false,
        builder: (ctx) => Dialog(
          elevation: 0, backgroundColor: Colors.white,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          child: Padding(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 70, height: 70,
                  decoration: const BoxDecoration(color: Color(0xFFFFF0F0), shape: BoxShape.circle),
                  child: const Icon(Icons.warning_rounded, color: Color(0xFFFF5B5B), size: 32),
                ),
                const SizedBox(height: 20),
                const Text("시작 위치가 아닙니다", style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                const SizedBox(height: 12),
                Text("코스 시작점과 거리가 너무 멉니다.\n(현재 거리: ${dist.toInt()}m)\n\n시작 위치로 이동해주세요.", textAlign: TextAlign.center, style: const TextStyle(fontSize: 15, color: Color(0xFF757575), height: 1.5)),
                const SizedBox(height: 30),
                SizedBox(
                  width: double.infinity, height: 52,
                  child: ElevatedButton(
                    onPressed: () => Navigator.pop(ctx),
                    style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFFFF5B5B), foregroundColor: Colors.white, elevation: 0, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16))),
                    child: const Text("확인", style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }
  }

  void _showStopDialog() {
    showDialog(
      context: context, barrierDismissible: false,
      builder: (ctx) => Dialog(
        elevation: 0, backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(width: 70, height: 70, decoration: BoxDecoration(color: primaryColor.withOpacity(0.1), shape: BoxShape.circle), child: const Center(child: Icon(Icons.check_circle_outline_rounded, color: primaryColor, size: 32))),
              const SizedBox(height: 20),
              const Text("러닝 종료", style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
              const SizedBox(height: 12),
              const Text("러닝을 종료하고\n기록을 저장하시겠습니까?", textAlign: TextAlign.center, style: TextStyle(fontSize: 15, color: Color(0xFF757575), height: 1.5)),
              const SizedBox(height: 30),
              Row(
                children: [
                  Expanded(child: SizedBox(height: 52, child: ElevatedButton(onPressed: () { Navigator.pop(ctx); _stopWatchTimer.onStartTimer(); }, style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFFF5F5F5), foregroundColor: const Color(0xFF757575), elevation: 0, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16))), child: const Text("계속 뛰기", style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700))))),
                  const SizedBox(width: 12),
                  Expanded(child: SizedBox(height: 52, child: ElevatedButton(onPressed: () { Navigator.pop(ctx); _saveRecord(); }, style: ElevatedButton.styleFrom(backgroundColor: primaryColor, foregroundColor: Colors.white, elevation: 0, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16))), child: const Text("종료", style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700))))),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _startRealRun() {
    setState(() => _isRunStarted = true);
    _stopWatchTimer.onStartTimer();
    _speak("러닝을 시작합니다. 안전하게 완주하세요!");
  }

  // ------------------------------------------------------------------------
  // 코스 경로 데이터 가져오기
  // ------------------------------------------------------------------------
  Future<void> _fetchCoursePath() async {
    if (widget.courseId == 0) return;
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('accessToken');
      final dio = Dio();
      final options = Options(headers: {'ngrok-skip-browser-warning': 'true', 'Authorization': 'Bearer $token'});

      final response = await dio.get('$baseUrl/api/v1/courses/${widget.courseId}', options: options);

      if (response.statusCode == 200) {
        final data = response.data;
        dynamic rawPathData = data['pathData'];
        List<dynamic> pathList = [];

        if (rawPathData is String) {
          try { pathList = jsonDecode(rawPathData); } catch (e) {}
        } else if (rawPathData is List) {
          pathList = rawPathData;
        }

        if (pathList.isNotEmpty) {
          List<LatLng> coursePoints = [];
          for (var p in pathList) {
            double lat = _toDouble(p['lat'] ?? p['latitude']);
            double lng = _toDouble(p['lng'] ?? p['longitude']);
            if (lat != 0.0 && lng != 0.0) coursePoints.add(LatLng(lat, lng));
          }

          if (mounted && coursePoints.isNotEmpty) {
            _courseStartPoint = coursePoints.first;
            // ★ 네비게이션용 경로 저장
            _globalCoursePoints = coursePoints;

            setState(() {
              _polylines.add(Polyline(polylineId: const PolylineId("course_guide"), points: coursePoints, color: Colors.grey.withOpacity(0.5), width: 8, zIndex: 1));
              _markers.add(Marker(markerId: const MarkerId("start"), position: coursePoints.first, icon: BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueGreen)));
              _markers.add(Marker(markerId: const MarkerId("end"), position: coursePoints.last, icon: BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueRed)));
            });

            Future.delayed(const Duration(milliseconds: 500), () async {
              try {
                final c = await _controller.future;
                c.animateCamera(CameraUpdate.newLatLngZoom(coursePoints.first, 16));
              } catch (_) {}
            });
          }
        }
      }
    } catch (e) {
      print("❌ 코스 로드 실패: $e");
    }
  }

  // ------------------------------------------------------------------------
  // 백그라운드 서비스 및 위치 업데이트
  // ------------------------------------------------------------------------
  Future<void> _startBackgroundService() async {
    if (kIsWeb) return;

    final service = FlutterBackgroundService();
    bool isRunning = await service.isRunning();
    if (!isRunning) await service.startService();

    await _serviceSubscription?.cancel();
    _serviceSubscription = service.on('update').listen((event) {
      if (event != null && mounted) {
        double lat = event['lat'] ?? 0.0;
        double lng = event['lng'] ?? 0.0;
        double speed = (event['speed'] ?? 0.0).toDouble();
        _updatePosition(lat, lng, speed);
      }
    });
  }

  void _updatePosition(double lat, double lng, double speed) async {
    LatLng newPos = LatLng(lat, lng);

    // ★ 진행 방향(Heading) 계산
    double currentHeading = 0.0;
    if (_currentPosition != null) {
      currentHeading = Geolocator.bearingBetween(
          _currentPosition!.latitude, _currentPosition!.longitude,
          lat, lng
      );
    }

    if (_currentPosition != null) {
      double distInMeters = Geolocator.distanceBetween(
        _currentPosition!.latitude, _currentPosition!.longitude,
        lat, lng,
      );

      if (distInMeters > 0) {
        setState(() {
          _totalDistance += (distInMeters / 1000);
          _calories = _totalDistance * 60;
          if (speed > 0) {
            double ps = 1000 / speed;
            _pace = "${(ps / 60).floor()}'${(ps % 60).floor().toString().padLeft(2, '0')}''";
          }
        });
      }
    }

    _myRouteCoords.add(newPos);

    setState(() {
      _currentPosition = Position(
          latitude: lat, longitude: lng, timestamp: DateTime.now(),
          accuracy: 0, altitude: 0, heading: currentHeading, speed: speed, speedAccuracy: 0, altitudeAccuracy: 0, headingAccuracy: 0
      );

      _polylines.removeWhere((p) => p.polylineId.value == "my_route");
      _polylines.add(Polyline(polylineId: const PolylineId("my_route"), points: _myRouteCoords, color: primaryColor, width: 6, zIndex: 2));
    });

    // ★ 네비게이션 로직 실행
    if (_isNaviOn && _globalCoursePoints.isNotEmpty) {
      _processNavigation(newPos, currentHeading);
    }

    try {
      final GoogleMapController controller = await _controller.future;
      controller.animateCamera(CameraUpdate.newLatLng(newPos));
    } catch (_) {}
  }

  // ★ 네비게이션 알고리즘
  void _processNavigation(LatLng currentPos, double currentHeading) {
    if (_globalCoursePoints.isEmpty) return;

    // 1. 가장 가까운 점 찾기
    int closestIndex = 0;
    double minDistance = double.infinity;
    for (int i = 0; i < _globalCoursePoints.length; i++) {
      double dist = Geolocator.distanceBetween(
          currentPos.latitude, currentPos.longitude,
          _globalCoursePoints[i].latitude, _globalCoursePoints[i].longitude
      );
      if (dist < minDistance) {
        minDistance = dist;
        closestIndex = i;
      }
    }

    // 2. 경로 이탈 (30m)
    if (minDistance > 30.0) {
      _speak("경로를 이탈했습니다. 코스로 돌아가세요.");
      return;
    }

    // 3. 방향 안내
    int targetIndex = closestIndex + 1;
    if (targetIndex < _globalCoursePoints.length) {
      LatLng targetPoint = _globalCoursePoints[targetIndex];
      double bearingToTarget = Geolocator.bearingBetween(
          currentPos.latitude, currentPos.longitude,
          targetPoint.latitude, targetPoint.longitude
      );

      double diff = bearingToTarget - currentHeading;
      if (diff > 180) diff -= 360;
      if (diff < -180) diff += 360;

      if (diff < -30) {
        _speak("왼쪽입니다.");
      } else if (diff > 30) {
        _speak("오른쪽입니다.");
      }
    } else {
      _speak("목적지가 얼마 남지 않았습니다.");
    }
  }

  // ------------------------------------------------------------------------
  // 기록 저장
  // ------------------------------------------------------------------------
  Future<void> _saveRecord() async {
    setState(() => _isSaving = true);
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('accessToken');
      final dio = Dio();
      final options = Options(headers: {'ngrok-skip-browser-warning': 'true', 'Authorization': 'Bearer $token', 'Content-Type': 'application/json'});

      List<Map<String, double>> routeJson = _myRouteCoords.map((e) => {"lat": e.latitude, "lng": e.longitude}).toList();



      final data = {
        "courseId": widget.courseId,
        "groupId": widget.groupId,
        "runTime": StopWatchTimer.getDisplayTime(_stopWatchTimer.rawTime.value, hours: true, milliSecond: false),
        "distance": double.parse(_totalDistance.toStringAsFixed(2)),
        "averagePace": _pace,
        "heartRate": 0,
        "calories": _calories.toInt(),
        "sectionJson": "[]",
        "routeData": jsonEncode(routeJson),
        "status": "COMPLETE"
      };

      final response = await dio.post('$baseUrl/api/v1/records', data: data, options: options);

      if (response.statusCode == 200 || response.statusCode == 201) {
        FlutterBackgroundService().invoke("stopService");
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("기록 저장 완료!")));
        Navigator.pop(context);
      }
    } catch (e) {
      if(mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("저장 실패")));
        setState(() => _isSaving = false);
      }
    }
  }

  // ------------------------------------------------------------------------
  // 헬스 데이터
  // ------------------------------------------------------------------------
  Future<void> _fetchHealthData() async {
    var types = [HealthDataType.HEART_RATE];
    List<HealthDataAccess> permissions = types.map((e) => HealthDataAccess.READ).toList();

    bool requested = await _health.requestAuthorization(types, permissions: permissions);

    if (requested) {
      Timer.periodic(const Duration(seconds: 5), (timer) async {
        if (!mounted) {
          timer.cancel();
          return;
        }
        DateTime now = DateTime.now();
        DateTime startTime = DateTime(now.year, now.month, now.day);
        try {
          List<HealthDataPoint> healthData = await _health.getHealthDataFromTypes(startTime: startTime, endTime: now, types: types);
          if (healthData.isNotEmpty) {
            var value = healthData.last.value;
            if (value is NumericHealthValue) setState(() => _heartRate = value.numericValue.toInt());
          }
        } catch (_) {}
      });
    }
  }

  double _toDouble(dynamic val) {
    if (val == null) return 0.0;
    if (val is double) return val;
    if (val is int) return val.toDouble();
    if (val is String) return double.tryParse(val) ?? 0.0;
    return 0.0;
  }

  // ------------------------------------------------------------------------
  // 화면 구성 (build)
  // ------------------------------------------------------------------------
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          // 1. 구글 맵
          GoogleMap(
            mapType: MapType.normal,
            initialCameraPosition: const CameraPosition(target: LatLng(37.5665, 126.9780), zoom: 15),
            myLocationEnabled: true,       // 파란 점 표시
            myLocationButtonEnabled: false, // 커스텀 버튼 사용을 위해 숨김
            zoomControlsEnabled: false,
            polylines: _polylines,
            markers: _markers,
            onMapCreated: (controller) => _controller.complete(controller),
          ),

          // 2. 상단 버튼 영역 (뒤로가기 + 토글 버튼)
          Positioned(
            top: 50, left: 16, right: 16,
            child: Row(
              children: [
                // ★ 뒤로가기 버튼
                GestureDetector(
                  onTap: () {
                    if (_isRunStarted) {
                      _showStopDialog();
                    } else {
                      Navigator.pop(context);
                    }
                  },
                  child: Container(
                    width: 45, height: 45,
                    decoration: BoxDecoration(
                      color: Colors.white,
                      shape: BoxShape.circle,
                      boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 4, spreadRadius: 2)],
                    ),
                    child: const Icon(Icons.arrow_back, color: Colors.black),
                  ),
                ),

                const SizedBox(width: 12),

                // 토글 버튼들
                Expanded(
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      _buildToggleChip("네비게이션", _isNaviOn, (val) {
                        setState(() => _isNaviOn = val);
                        if (val) _speak("네비게이션을 시작합니다.");
                      }),
                      const SizedBox(width: 8),
                      _buildToggleChip("AI 코치", _isAiCoachOn, (val) {
                        setState(() => _isAiCoachOn = val);
                      }),
                    ],
                  ),
                ),
              ],
            ),
          ),

          // 3. ★ 현위치 재조정 버튼 (정보창 위)
          Positioned(
            right: 20,
            bottom: 360,
            child: FloatingActionButton(
              heroTag: "myloc",
              onPressed: _moveToCurrentLocation,
              backgroundColor: Colors.white,
              foregroundColor: primaryColor,
              elevation: 4,
              shape: const CircleBorder(),
              child: const Icon(Icons.my_location, size: 28),
            ),
          ),

          // 4. 하단 정보창
          Positioned(
            bottom: 0, left: 0, right: 0,
            child: Container(
              padding: const EdgeInsets.fromLTRB(24, 30, 24, 40),
              decoration: const BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.vertical(top: Radius.circular(30)),
                boxShadow: [BoxShadow(color: Colors.black12, blurRadius: 20, spreadRadius: 5)],
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text("${_totalDistance.toStringAsFixed(2)} km", style: const TextStyle(fontSize: 40, fontWeight: FontWeight.bold, fontFamily: "Monospace")),
                  StreamBuilder<int>(
                    stream: _stopWatchTimer.rawTime, initialData: 0,
                    builder: (context, snap) {
                      return Text(StopWatchTimer.getDisplayTime(snap.data!, hours: true, milliSecond: false), style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w500));
                    },
                  ),
                  const SizedBox(height: 15),

                  // Replay & User Info
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 10),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        ElevatedButton.icon(
                          style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF2C3E50), foregroundColor: Colors.white, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)), padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8), elevation: 0, minimumSize: Size.zero, tapTargetSize: MaterialTapTargetSize.shrinkWrap),
                          icon: const Icon(Icons.play_circle_outline, size: 16),
                          label: const Text("Replay", style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold)),
                          onPressed: () {},
                        ),
                        const Text("열쩡열쩡", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                        const SizedBox(width: 80),
                      ],
                    ),
                  ),
                  const SizedBox(height: 15),
                  Row(mainAxisAlignment: MainAxisAlignment.spaceAround, children: [_buildStatItem("페이스", _pace), _buildStatItem("칼로리", "${_calories.toInt()} kcal"), _buildStatItem("심박수", "$_heartRate bpm")]),
                  const SizedBox(height: 30),

                  // START/STOP 버튼
                  SizedBox(
                    width: double.infinity, height: 55,
                    child: ElevatedButton(
                      onPressed: _isSaving ? null : () {
                        if (!_isRunStarted) _tryStartRun(); else { _stopWatchTimer.onStopTimer(); _showStopDialog(); }
                      },
                      style: ElevatedButton.styleFrom(backgroundColor: _isRunStarted ? primaryColor : Colors.green, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30))),
                      child: _isSaving ? const CircularProgressIndicator(color: Colors.white) : Text(_isRunStarted ? "STOP" : "START", style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
                    ),
                  )
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatItem(String l, String v) => Column(children: [Text(v, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)), const SizedBox(height: 4), Text(l, style: const TextStyle(color: Colors.grey, fontSize: 12))]);

  Widget _buildToggleChip(String l, bool isOn, Function(bool) c) => GestureDetector(
    onTap: () => c(!isOn),
    child: Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(color: isOn ? primaryColor : Colors.white, borderRadius: BorderRadius.circular(20), boxShadow: [const BoxShadow(color: Colors.black12, blurRadius: 4)]),
      child: Row(children: [Icon(Icons.directions_run, size: 16, color: isOn ? Colors.white : Colors.black), const SizedBox(width: 8), Text(l, style: TextStyle(color: isOn ? Colors.white : Colors.black, fontWeight: FontWeight.bold))]),
    ),
  );
}