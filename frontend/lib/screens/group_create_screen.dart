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
  // [원본 유지] 검색 관련 변수들
  final _startSearchController = TextEditingController();
  final _endSearchController = TextEditingController();
  dynamic _searchedCourse;
  bool _hasSearched = false;
  List<dynamic> _startPoiList = [];
  List<dynamic> _endPoiList = [];
  Map<String, double>? _startCoord;
  Map<String, double>? _endCoord;

  final _nameController = TextEditingController();
  final _descController = TextEditingController();
  final _tagController = TextEditingController();

  double _maxPeople = 10;
  DateTime _startDate = DateTime.now();
  DateTime _endDate = DateTime.now().add(const Duration(days: 7));

  bool _isSecret = false;
  bool _isLoading = false;

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
    _fetchAllCourses();
  }

  String _generateRandomAccessCode() {
    var rng = Random();
    return rng.nextInt(100000000).toString().padLeft(8, '0');
  }

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
          _isMapLoading = false; // 자동 선택을 지우고 로딩만 꺼줍니다.
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
    } else {
      setState(() => _isMapLoading = false);
    }
  }

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

      final Map<String, dynamic> data = {
        "groupName": _nameController.text,
        "description": _descController.text,
        "tags": _tagController.text,
        "maxPeople": _maxPeople.toInt(),
        "startDate": startStr,
        "endDate": endStr,
        "isSecret": _isSecret,
        "isSearchable": !_isSecret,
        "accessCode": _isSecret ? myRandomCode : null,
      };

      if (_hasSearched && _selectedCourseId == -1) {
        data["courseTitle"] = _searchedCourse['title'];
        data["pathData"] = jsonEncode(_searchedCourse['pathData']);
        data["distance"] = _searchedCourse['distance'];
        data["expectedTime"] = _searchedCourse['expectedTime'];
      } else {
        data["courseId"] = _selectedCourseId;
      }

      final response = await dio.post(groupUrl, data: data, options: options);

      if (response.statusCode == 200 || response.statusCode == 201) {
        if (_isSecret) {
          if (!mounted) return;
          final resData = response.data;
          String realCode = resData['accessCode'] ?? myRandomCode;
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

  Future<void> _fetchPoiList(String keyword, bool isStart) async {
    if (keyword.length < 2) {
      setState(() { if (isStart) _startPoiList = []; else _endPoiList = []; });
      return;
    }
    try {
      final dio = Dio();
      final response = await dio.get(
        '$baseUrl/api/v1/courses/poi-search',
        queryParameters: {'keyword': keyword},
        options: Options(headers: {'ngrok-skip-browser-warning': 'true'}),
      );
      setState(() {
        if (isStart) _startPoiList = response.data;
        else _endPoiList = response.data;
      });
    } catch (e) { print("POI 검색 에러: $e"); }
  }

  Future<void> _searchNewPath() async {
    if (_startSearchController.text.isEmpty || _endSearchController.text.isEmpty) return;
    setState(() => _isMapLoading = true);
    try {
      final dio = Dio();
      final response = await dio.get(
        '$baseUrl/api/v1/courses/search',
        queryParameters: {'startName': _startSearchController.text, 'endName': _endSearchController.text},
        options: Options(headers: {'ngrok-skip-browser-warning': 'true'}),
      );
      if (response.data != null) {
        _searchedCourse = response.data;
        _hasSearched = true;
        _selectedCourseId = -1;
        _onCourseSelected(_searchedCourse);
      }
    } catch (e) { print("❌ 검색 실패: $e"); } finally { setState(() => _isMapLoading = false); }
  }

  void _showInviteCodeDialog(String code) { /* 기존 팝업 로직 */ }

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
            _label('대회명 *'), TextField(controller: _nameController, decoration: _inputDeco('대회명을 입력해주세요.')), const SizedBox(height: 20),
            _label('대회 소개'), TextField(controller: _descController, decoration: _inputDeco('대회 소개를 입력해주세요.')), const SizedBox(height: 10), TextField(controller: _tagController, decoration: _inputDeco('#태그 추가')), const SizedBox(height: 20),
            _label('대회 인원'),
            Row(children: [
              Expanded(child: Slider(value: _maxPeople, min: 2, max: 50, divisions: 48, activeColor: primaryColor, onChanged: (val) => setState(() => _maxPeople = val))),
              Text("${_maxPeople.toInt()}명", style: const TextStyle(fontWeight: FontWeight.bold))
            ]), const SizedBox(height: 20),
            _label('기간 설정'), Row(children: [Expanded(child: _dateSelector(true)), const Padding(padding: EdgeInsets.symmetric(horizontal: 8), child: Text("~")), Expanded(child: _dateSelector(false))]), const SizedBox(height: 20),
            _label('공개 설정'), Row(children: [Flexible(child: _buildRadio('공개', false)), Flexible(child: _buildRadio('비공개', true))]), const SizedBox(height: 30),

            _label('코스 선택 *'),
            const SizedBox(height: 10),
            _label('AI 추천 코스'),

            // ★ [수정됨] 드롭다운 버튼 대신 리스트 위젯 호출
            _buildCourseList(),

            const SizedBox(height: 25),
            _label('직접 경로 검색'),
            Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(color: Colors.grey[50], borderRadius: BorderRadius.circular(20), border: Border.all(color: Colors.grey.shade200)),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    TextField(controller: _startSearchController, decoration: _inputDeco('출발지를 입력하세요.'), onChanged: (val) => _fetchPoiList(val, true)),
                    // ★ [원본 유지] 직접 검색 결과 리스트
                    if (_startPoiList.isNotEmpty) _buildPoiListView(_startPoiList, true),
                    const SizedBox(height: 12),
                    TextField(controller: _endSearchController, textInputAction: TextInputAction.search, decoration: _inputDeco('도착지를 입력하세요.'), onChanged: (val) => _fetchPoiList(val, false), onSubmitted: (_) => _searchNewPath()),
                    // ★ [원본 유지] 직접 검색 결과 리스트
                    if (_endPoiList.isNotEmpty) _buildPoiListView(_endPoiList, false),
                  ],
                )
            ),

            if (_hasSearched) ...[
              const SizedBox(height: 20),
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(20), boxShadow: [BoxShadow(color: Colors.black.withOpacity(0.08), blurRadius: 15, offset: const Offset(0, 5))], border: Border.all(color: Colors.orange.shade200)),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [const Text("🔍 검색된 추천 경로", style: TextStyle(color: Colors.orange, fontWeight: FontWeight.bold, fontSize: 12)), Text(_searchedCourse['expectedTime'] ?? "", style: const TextStyle(fontWeight: FontWeight.bold))]),
                    const SizedBox(height: 8),
                    Text(_searchedCourse['title'] ?? "", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                    const SizedBox(height: 4),
                    Text("총 거리: ${_searchedCourse['distance'] ?? ""}", style: const TextStyle(color: Colors.grey)),
                  ],
                ),
              ),
            ],

            const SizedBox(height: 30),

            _label('코스 미리보기'),
            Container(
              decoration: BoxDecoration(border: Border.all(color: Colors.grey.shade300), borderRadius: BorderRadius.circular(16)),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: SizedBox(
                  height: 350, width: double.infinity,
                  child: _isMapLoading
                      ? const Center(child: CircularProgressIndicator())
                      : GoogleMap(
                    initialCameraPosition: CameraPosition(target: _initialPosition, zoom: 14),
                    zoomControlsEnabled: false,
                    myLocationButtonEnabled: false, // ★ 이 줄을 추가하여 현위치 버튼을 숨깁니다.
                    myLocationEnabled: false,       // 현위치 파란 점도 필요 없다면 false
                    padding: const EdgeInsets.all(50),
                    polylines: _polylines,
                    markers: _markers,
                    onMapCreated: (GoogleMapController c) async { // ★ async 추가
                      if (!_mapController.isCompleted) {
                        _mapController.complete(c);
                      }
                      // ★ 추가: 코스가 이미 선택되어 있다면 (폴리라인이 있다면) 카메라 영역을 맞춥니다.
                      if (_polylines.isNotEmpty) {
                        // 지도가 렌더링될 시간을 0.3초 정도만 줍니다.
                        await Future.delayed(const Duration(milliseconds: 300));

                        // 모든 포인트를 계산해서 카메라를 이동시킵니다.
                        c.animateCamera(
                          CameraUpdate.newLatLngBounds(
                              _createBounds(_polylines.first.points),
                              3.0 // 여백 (출도착지가 멀면 숫자를 키우세요)
                          ),
                        );
                      }
                    },
                  ),
                ),
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

  // ★ [신규] 렌더링 에러가 없는 Column 방식의 AI 추천 코스 상세 리스트
  // ★ [수정됨] 에러 유발 요소를 모두 제거한 안전한 리스트 위젯
  Widget _buildCourseList() {
    if (_courseList.isEmpty) {
      return const Text("추천 코스가 없습니다.", style: TextStyle(color: Colors.grey));
    }

    // 딱 3개만 보여주기
    final displayList = _courseList.take(3).toList();

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF8F9FA),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (var course in displayList) ...[
            // 1. 제목과 선택 버튼 (가로 크기 고정으로 에러 방지)
            Row(
              children: [
                Expanded(
                  child: Text(
                    course['title'] ?? "이름 없는 코스",
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                ),
                const SizedBox(width: 10),
                // 버튼의 크기를 명확히 제한하여 'Infinite Width' 에러 차단
                SizedBox(
                  width: 80,
                  height: 32,
                  child: ElevatedButton(
                    onPressed: () => _onCourseSelected(course),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _selectedCourseId == course['id']
                          ? Colors.grey
                          : const Color(0xFFFF8A5C),
                      padding: EdgeInsets.zero,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
                      elevation: 0,
                    ),
                    child: Text(
                      _selectedCourseId == course['id'] ? "선택됨" : "선택",
                      style: const TextStyle(color: Colors.white, fontSize: 11),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            // 2. 거리 및 시간 정보
            Text(
              "거리: 약 ${course['distance'] ?? '0'}km | 시간: 약 ${course['expectedTime'] ?? '0'}분",
              style: const TextStyle(color: Colors.orange, fontWeight: FontWeight.bold, fontSize: 13),
            ),
            const SizedBox(height: 4),
            // 3. 코스 설명
            Text(
              course['description'] ?? "코스 설명이 없습니다.",
              style: const TextStyle(color: Colors.grey, fontSize: 12),
            ),
            // 마지막 아이템이 아니면 구분선 추가
            if (course != displayList.last)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 15),
                child: Divider(height: 1),
              ),
          ]
        ],
      ),
    );
  }

  // ★ [원본 로직 100% 유지] 검색 POI 리스트 위젯
  Widget _buildPoiListView(List<dynamic> list, bool isStart) {
    return Material(
      elevation: 8, borderRadius: BorderRadius.circular(12),
      child: Container(
        constraints: const BoxConstraints(maxHeight: 250),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(12), border: Border.all(color: Colors.grey.shade300)),
        child: ListView.separated(
          padding: EdgeInsets.zero, shrinkWrap: true, itemCount: list.length, separatorBuilder: (ctx, i) => const Divider(height: 1),
          itemBuilder: (ctx, i) => ListTile(
            dense: true, leading: const Icon(Icons.location_on, color: Colors.orange, size: 20),
            title: Text(list[i]['name'], style: const TextStyle(fontWeight: FontWeight.bold)),
            subtitle: Text(list[i]['address'], style: const TextStyle(fontSize: 11)),
            onTap: () {
              setState(() {
                if (isStart) {
                  _startSearchController.text = list[i]['name'];
                  _startCoord = {'lat': list[i]['lat'], 'lng': list[i]['lng']};
                  _startPoiList = [];
                } else {
                  _endSearchController.text = list[i]['name'];
                  _endCoord = {'lat': list[i]['lat'], 'lng': list[i]['lng']};
                  _endPoiList = [];
                  _searchNewPath();
                }
              });
            },
          ),
        ),
      ),
    );
  }

  Widget _label(String t) => Padding(padding: const EdgeInsets.only(bottom: 8), child: Text(t, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold)));
  InputDecoration _inputDeco(String h) => InputDecoration(hintText: h, filled: true, fillColor: Colors.grey[100], border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none), contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14));
  Widget _buildRadio(String label, bool value) => Row(children: [Radio<bool>(value: value, groupValue: _isSecret, activeColor: primaryColor, onChanged: (val) => setState(() => _isSecret = val!)), Text(label)]);
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