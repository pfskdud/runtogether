import 'dart:async';
import 'dart:convert';
import 'dart:math';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import '../constants.dart';

class GroupCreateScreen extends StatefulWidget {
  const GroupCreateScreen({super.key});

  @override
  State<GroupCreateScreen> createState() => _GroupCreateScreenState();
}

class _GroupCreateScreenState extends State<GroupCreateScreen> {
  // [원본 유지] 모든 컨트롤러들
  final _nameController = TextEditingController();
  final _descController = TextEditingController();
  final _tagController = TextEditingController();

  // [원본 유지] 설정 변수들
  double _maxPeople = 10;
  DateTime _startDate = DateTime.now();
  DateTime _endDate = DateTime.now().add(const Duration(days: 7));

  bool _isSecret = false;
  bool _isLoading = false;

  // ★ [수정됨] 고정 ID 10번 대신 리스트와 선택된 ID 관리
  List<dynamic> _courseList = [];
  int? _selectedCourseId;
  String _selectedCourseName = "로딩 중...";

  final Completer<GoogleMapController> _mapController = Completer();
  Set<Polyline> _polylines = {};
  Set<Marker> _markers = {};
  LatLng _initialPosition = const LatLng(37.5665, 126.9780);
  bool _isMapLoading = true;

  @override
  void initState() {
    super.initState();
    _fetchAllCourses(); // ★ 고정 코스 대신 전체 목록을 가져옵니다.
  }

  String _generateRandomAccessCode() {
    var rng = Random();
    return rng.nextInt(100000000).toString().padLeft(8, '0');
  }

  // ★ [수정됨] 전체 코스 정보 가져오기
  Future<void> _fetchAllCourses() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('accessToken');
      final dio = Dio();
      final options = Options(headers: {
        'ngrok-skip-browser-warning': 'true',
        'Authorization': 'Bearer $token'
      });

      final url = '$baseUrl/api/v1/courses';
      final response = await dio.get(url, options: options);

      if (response.statusCode == 200) {
        final rawData = response.data;
        final List<dynamic> data = (rawData is Map && rawData.containsKey('data')) ? rawData['data'] : rawData;

        setState(() {
          _courseList = data;
          if (_courseList.isNotEmpty) {
            _onCourseSelected(_courseList[0]); // 첫 번째 코스 자동 선택
          } else {
            _isMapLoading = false;
          }
        });
      }
    } catch (e) {
      print("❌ 코스 목록 로드 실패: $e");
      setState(() {
        _selectedCourseName = "코스 정보를 불러올 수 없습니다.";
        _isMapLoading = false;
      });
    }
  }

  // ★ [추가됨] 코스 선택 시 지도와 이름을 업데이트하는 함수
  void _onCourseSelected(dynamic course) {
    setState(() {
      _selectedCourseId = course['id'];
      _selectedCourseName = course['title'] ?? course['courseName'] ?? "이름 없는 코스";
      _isMapLoading = true;
    });

    final pathData = course['pathData'] ?? course['path'] ?? course['route'];
    _drawRouteOnMap(pathData);
  }

  LatLngBounds _createBounds(List<LatLng> positions) {
    final southwestLat = positions.map((p) => p.latitude).reduce((curr, next) => curr < next ? curr : next);
    final southwestLon = positions.map((p) => p.longitude).reduce((curr, next) => curr < next ? curr : next);
    final northeastLat = positions.map((p) => p.latitude).reduce((curr, next) => curr > next ? curr : next);
    final northeastLon = positions.map((p) => p.longitude).reduce((curr, next) => curr > next ? curr : next);
    return LatLngBounds(
      southwest: LatLng(southwestLat, southwestLon),
      northeast: LatLng(northeastLat, northeastLon),
    );
  }

  void _drawRouteOnMap(dynamic rawPathData) {
    if (rawPathData == null) {
      setState(() => _isMapLoading = false);
      return;
    }
    List<LatLng> points = [];
    try {
      List<dynamic> list = (rawPathData is String) ? jsonDecode(rawPathData) : rawPathData;
      for (var p in list) {
        double lat = double.tryParse(p['lat']?.toString() ?? p['latitude']?.toString() ?? "0") ?? 0.0;
        double lng = double.tryParse(p['lng']?.toString() ?? p['longitude']?.toString() ?? "0") ?? 0.0;
        if (lat != 0 && lng != 0) points.add(LatLng(lat, lng));
      }
    } catch (e) { print("파싱 에러: $e"); }

    if (points.isNotEmpty) {
      setState(() {
        _initialPosition = points.first;
        _polylines = {
          Polyline(
            polylineId: const PolylineId("course_path"),
            points: points,
            color: Colors.blueAccent,
            width: 5,
            jointType: JointType.round,
            startCap: Cap.roundCap,
            endCap: Cap.roundCap,
          )
        };
        _markers = {
          Marker(markerId: const MarkerId("start"), position: points.first, infoWindow: const InfoWindow(title: "출발"), icon: BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueGreen)),
          Marker(markerId: const MarkerId("end"), position: points.last, infoWindow: const InfoWindow(title: "도착"), icon: BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueRed)),
        };
        _isMapLoading = false;
      });
      _mapController.future.then((c) {
        Future.delayed(const Duration(milliseconds: 300), () {
          try { c.animateCamera(CameraUpdate.newLatLngBounds(_createBounds(points), 50.0)); } catch (_) {}
        });
      });
    } else {
      setState(() => _isMapLoading = false);
    }
  }

  // [수정됨] 그룹 생성 요청
  void _createGroup() async {
    if (_nameController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('그룹 이름을 입력해주세요.')));
      return;
    }
    if (_selectedCourseId == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('코스를 선택해주세요.')));
      return;
    }

    setState(() => _isLoading = true);

    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('accessToken');
      final dio = Dio();
      final options = Options(headers: {
        'ngrok-skip-browser-warning': 'true',
        'Authorization': 'Bearer $token',
        'Content-Type': 'application/json'
      });

      String startStr = "${_startDate.year}-${_startDate.month.toString().padLeft(2,'0')}-${_startDate.day.toString().padLeft(2,'0')}";
      String endStr = "${_endDate.year}-${_endDate.month.toString().padLeft(2,'0')}-${_endDate.day.toString().padLeft(2,'0')}";

      String myRandomCode = _isSecret ? _generateRandomAccessCode() : "";

      // [원본 유지] 모든 데이터 포함
      final data = {
        "groupName": _nameController.text,
        "description": _descController.text,
        "tags": _tagController.text,
        "maxPeople": _maxPeople.toInt(),
        "startDate": startStr,
        "endDate": endStr,
        "isSecret": _isSecret,
        "isSearchable": !_isSecret,
        "courseId": _selectedCourseId, // ★ 선택된 ID로 변경
        "accessCode": _isSecret ? myRandomCode : null,
      };

      final response = await dio.post(groupUrl, data: data, options: options);

      if (response.statusCode == 200 || response.statusCode == 201) {
        if (_isSecret) {
          if (!mounted) return;
          final resData = response.data;
          String realCode = "";
          if (resData['accessCode'] != null) realCode = resData['accessCode'];
          else if (resData['data'] != null && resData['data']['accessCode'] != null) realCode = resData['data']['accessCode'];
          else if (resData['message'] != null && resData['message'].contains("[입장코드:")) {
            int start = resData['message'].indexOf(":") + 1;
            int end = resData['message'].indexOf("]");
            realCode = resData['message'].substring(start, end).trim();
          }
          if (realCode.isEmpty) realCode = myRandomCode;
          _showInviteCodeDialog(realCode);
        } else {
          if (!mounted) return;
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('대회가 생성되었습니다!')));
          Navigator.pop(context);
        }
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('오류가 발생했습니다.')));
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  // [원본 유지] 비공개 코드 팝업
  void _showInviteCodeDialog(String code) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        icon: Container(
          margin: const EdgeInsets.only(top: 10),
          width: 80, height: 80,
          decoration: BoxDecoration(color: Colors.orange.withOpacity(0.1), shape: BoxShape.circle),
          child: const Icon(Icons.check_circle_rounded, size: 48, color: primaryColor),
        ),
        title: const Text("비공개 대회 생성 완료", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20), textAlign: TextAlign.center),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text("아래 입장 코드를 참가자들에게 공유하세요.", style: TextStyle(color: Colors.grey, fontSize: 14)),
            const SizedBox(height: 20),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(color: const Color(0xFFF5F6F8), borderRadius: BorderRadius.circular(12), border: Border.all(color: Colors.grey.shade200)),
              child: Row(
                children: [
                  Expanded(child: SelectableText(code, textAlign: TextAlign.center, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold))),
                  IconButton(icon: const Icon(Icons.content_copy_rounded, color: primaryColor), onPressed: () {
                    Clipboard.setData(ClipboardData(text: code));
                    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("코드가 복사되었습니다!")));
                  }),
                ],
              ),
            ),
          ],
        ),
        actions: [
          SizedBox(width: double.infinity, child: TextButton(onPressed: () { Navigator.pop(ctx); Navigator.pop(context); }, style: TextButton.styleFrom(backgroundColor: primaryColor, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)), padding: const EdgeInsets.symmetric(vertical: 14)), child: const Text("확인", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)))),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('대회 생성', style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold)), backgroundColor: Colors.white, elevation: 0, leading: const BackButton(color: Colors.black)),
      backgroundColor: Colors.white,
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // [원본 유지] 대회명, 소개, 태그 필드
            _label('대회명 *'), TextField(controller: _nameController, decoration: _inputDeco('대회명을 입력해주세요.')), const SizedBox(height: 20),
            _label('대회 소개'), TextField(controller: _descController, decoration: _inputDeco('대회 소개를 입력해주세요.')), const SizedBox(height: 10), TextField(controller: _tagController, decoration: _inputDeco('#태그 추가')), const SizedBox(height: 20),

            // [원본 유지] 인원 슬라이더
            _label('대회 인원'),
            Row(children: [
              Expanded(child: Slider(value: _maxPeople, min: 2, max: 50, divisions: 48, activeColor: primaryColor, onChanged: (val) => setState(() => _maxPeople = val))),
              Text("${_maxPeople.toInt()}명", style: const TextStyle(fontWeight: FontWeight.bold))
            ]), const SizedBox(height: 20),

            // [원본 유지] 기간 설정
            _label('기간 설정'), Row(children: [Expanded(child: _dateSelector(true)), const Padding(padding: EdgeInsets.symmetric(horizontal: 8), child: Text("~")), Expanded(child: _dateSelector(false))]), const SizedBox(height: 20),

            // [원본 유지] 공개 설정
            _label('공개 설정'),
            Row(children: [
              Flexible(child: _buildRadio('공개', false)),
              Flexible(child: _buildRadio('비공개', true)),
            ]),
            const SizedBox(height: 30),

            // ★ [추가됨] 코스 선택 드롭다운 (고정 텍스트 영역 대체)
            _label('코스 선택 *'),

            _buildCourseSelector(),
            const SizedBox(height: 20),

            // [원본 유지] 지도 미리보기
            _label('코스 미리보기'),
            Container(
              decoration: BoxDecoration(border: Border.all(color: Colors.grey.shade300), borderRadius: BorderRadius.circular(16)),
              child: Column(
                children: [
                  SizedBox(
                    height: 350, width: double.infinity,
                    child: _isMapLoading
                        ? const Center(child: CircularProgressIndicator())
                        : GoogleMap(initialCameraPosition: CameraPosition(target: _initialPosition, zoom: 14), zoomControlsEnabled: false, polylines: _polylines, markers: _markers, onMapCreated: (c) => _mapController.complete(c)),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 40),
            SizedBox(width: double.infinity, height: 50, child: ElevatedButton(onPressed: _isLoading ? null : _createGroup, style: ElevatedButton.styleFrom(backgroundColor: primaryColor, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12))), child: _isLoading ? const CircularProgressIndicator(color: Colors.white) : const Text('대회 생성 완료', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)))),
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }

  // 코스 선택 버튼 (드롭다운 방식)
  Widget _buildCourseSelector() {
    return PopupMenuButton<dynamic>(
      // 1. 코스 선택 시 실행될 로직
      onSelected: (course) {
        _onCourseSelected(course);
      },
      // 2. 버튼 모양 (기존의 깔끔한 디자인 유지 + 가로 꽉 차게)
      child: Container(
        width: double.infinity, // 가로로 꽉 차게 설정
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.grey.shade300),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Expanded( // 이름이 길면 ...으로 표시
              child: Text(
                _selectedCourseName,
                style: TextStyle(
                    fontSize: 14,
                    color: _selectedCourseId == null ? Colors.grey : Colors.black87
                ),
                overflow: TextOverflow.ellipsis,
              ),
            ),
            const Icon(Icons.keyboard_arrow_down_rounded, color: Colors.grey),
          ],
        ),
      ),
      // 3. 드롭다운 메뉴 스타일
      color: Colors.white,
      elevation: 4,
      offset: const Offset(0, 50), // 버튼 바로 아래에 열리도록 조정
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      // 4. 리스트 아이템 생성
      itemBuilder: (context) {
        if (_courseList.isEmpty) {
          return [
            const PopupMenuItem(enabled: false, child: Text("불러온 코스가 없습니다."))
          ];
        }
        return _courseList.map((course) {
          final isSelected = _selectedCourseId == course['id'];
          return PopupMenuItem<dynamic>(
            value: course,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    course['title'] ?? course['courseName'] ?? "이름 없음",
                    style: TextStyle(
                      fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                      color: isSelected ? primaryColor : Colors.black87,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                if (isSelected)
                  const Icon(Icons.check_circle_rounded, color: primaryColor, size: 20),
              ],
            ),
          );
        }).toList();
      },
    );
  }

  // [원본 유지] 헬퍼 메서드들
  Widget _label(String text) => Padding(padding: const EdgeInsets.only(bottom: 8.0), child: Text(text, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold)));
  InputDecoration _inputDeco(String hint) => InputDecoration(hintText: hint, filled: true, fillColor: Colors.grey[100], border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none), contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14));

  Widget _buildRadio(String label, bool value) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Radio<bool>(value: value, groupValue: _isSecret, activeColor: primaryColor, onChanged: (val) => setState(() => _isSecret = val!)),
        Text(label, style: const TextStyle(fontSize: 13)),
      ],
    );
  }

  Widget _dateSelector(bool isStart) {
    final date = isStart ? _startDate : _endDate;
    return GestureDetector(
      onTap: () async {
        final DateTime? picked = await showDatePicker(context: context, initialDate: date, firstDate: DateTime(2025), lastDate: DateTime(2030));
        if (picked != null) setState(() { if (isStart) _startDate = picked; else _endDate = picked; });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(12), border: Border.all(color: Colors.grey.shade300)),
        child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [Text("${date.year}-${date.month}-${date.day}"), const Icon(Icons.calendar_today, size: 16, color: Colors.grey)]),
      ),
    );
  }
}