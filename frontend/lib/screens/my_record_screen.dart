import 'dart:convert'; // jsonDecode용
import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../constants.dart';
import 'replay_screen.dart'; // 리플레이 화면 연결용 (필요시 import 확인)

class MyRecordScreen extends StatefulWidget {
  final int? recordId;   // 특정 기록 ID (상세 조회용)
  final bool isEmbedded; // 탭 안에 포함된 경우인지
  final int? groupId;    // ★ [추가] 그룹 ID (그룹 내 최고 기록 조회용)

  const MyRecordScreen({
    super.key,
    this.recordId,
    required this.isEmbedded,
    this.groupId,        // ★ [추가] 생성자
  });

  @override
  State<MyRecordScreen> createState() => _MyRecordScreenState();
}

class _MyRecordScreenState extends State<MyRecordScreen> {
  bool _isLoading = true;
  String? _errorMessage;

  // [기본값 설정]
  Map<String, dynamic> _recordData = {
    "groupName": "내 기록",
    "date": "-",
    "startTime": "-",
    "runTime": "00:00",
    "distance": 0.0,
    "avgPace": "-'--''",
    "calories": 0,
    "heartRate": 0,
    "sectionJson": [],
    "myRank": 0,
    "totalRunners": 0,
    "groupAvgPace": "-'--''",
    "paceDifference": "-",
    "analysisResult": "분석 데이터가 없습니다.",
    "badges": [],
  };

  @override
  void initState() {
    super.initState();
    _fetchRecord();
  }

  // [API] 서버 통신 함수
  Future<void> _fetchRecord() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });

    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('accessToken');

      // ★★★ [핵심 수정] URL 분기 처리 ★★★
      String endpoint;

      if (widget.recordId != null) {
        // 1. 특정 기록 조회 (기록 리스트에서 클릭했을 때)
        endpoint = '$baseUrl/api/v1/records/${widget.recordId}';
      }
      else if (widget.groupId != null && widget.groupId != 0) {
        // 2. [NEW] 그룹 내 최고 기록 조회 (그룹 상세 -> 내 기록 탭)
        endpoint = '$baseUrl/api/v1/groups/${widget.groupId}/records/best';
      }
      else {
        // 3. 전체 최신 기록 조회 (마이페이지 -> 내 기록)
        endpoint = '$baseUrl/api/v1/records/latest';
      }

      print("🚀 내 기록 요청 URL: $endpoint");

      final dio = Dio();
      final response = await dio.get(
        endpoint,
        options: Options(headers: {
          'Authorization': 'Bearer $token',
          'ngrok-skip-browser-warning': 'true'
        }),
      );

      // 200 OK (성공)
      if (response.statusCode == 200 && response.data != null) {
        final data = response.data;

        // (로그 출력용 복사본)
        try {
          Map<String, dynamic> logData = Map.from(data);
          if (logData['routeData'] is List) {
            int count = (logData['routeData'] as List).length;
            logData['routeData'] = "📍 좌표 $count개 (생략)";
          }
          if (logData['sectionJson'] is String && (logData['sectionJson'] as String).length > 50) {
            logData['sectionJson'] = "📊 구간 데이터 (생략)";
          }
          print("✅ 데이터 수신 성공: $logData");
        } catch (_) {}

        // sectionJson 파싱
        dynamic sections = data['sectionJson'];
        if (sections is String) {
          try { sections = jsonDecode(sections); } catch (e) { sections = []; }
        }

        if (mounted) {
          setState(() {
            _recordData = data;
            _recordData['sectionJson'] = sections ?? [];
          });
        }
      }
      // 204 No Content (기록 없음)
      else if (response.statusCode == 204) {
        if (mounted) setState(() => _errorMessage = "아직 이 대회에서의 기록이 없습니다.\n첫 기록을 달성해보세요!");
      }
      else {
        if (mounted) setState(() => _errorMessage = "기록을 불러오지 못했습니다.");
      }
    } catch (e) {
      print("❌ 에러 발생: $e");
      // 에러 발생 시 (404 등)
      if (mounted) {
        setState(() => _errorMessage = "아직 기록이 없습니다.");
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  // 페이스 문자열 -> Double 변환
  double _parsePaceToDouble(String? paceStr) {
    if (paceStr == null || !paceStr.contains(":")) return 0.0;
    try {
      final parts = paceStr.split(":");
      double min = double.parse(parts[0]);
      double sec = double.parse(parts[1]);
      return min + (sec / 60);
    } catch (e) {
      return 0.0;
    }
  }

  @override
  Widget build(BuildContext context) {
    final sections = (_recordData['sectionJson'] is List) ? _recordData['sectionJson'] as List<dynamic> : [];
    final badges = (_recordData['badges'] is List) ? _recordData['badges'] as List<dynamic> : [];

    return Scaffold(
      backgroundColor: Colors.grey[100],
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: primaryColor))
          : _errorMessage != null
          ? Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.history_toggle_off, size: 60, color: Colors.grey),
            const SizedBox(height: 16),
            Text(_errorMessage!, textAlign: TextAlign.center, style: const TextStyle(color: Colors.grey, fontSize: 16)),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: _fetchRecord,
              style: ElevatedButton.styleFrom(backgroundColor: primaryColor),
              child: const Text("다시 시도", style: TextStyle(color: Colors.white)),
            )
          ],
        ),
      )
          : SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            // 1. 러닝 요약
            _buildSummaryCard(),
            const SizedBox(height: 16),

            // 2. 구간별 기록
            if (sections.isNotEmpty) ...[
              _buildLapTableCard(sections),
              const SizedBox(height: 16),
              _buildPaceGraphCard(sections),
              const SizedBox(height: 16),
            ],

            // 3. 그룹 비교
            _buildComparisonCard(),
            const SizedBox(height: 16),

            // 4. 분석 결과
            _buildAnalysisCard(),
            const SizedBox(height: 16),

            // 5. 배지
            _buildBadgeCard(badges),
            const SizedBox(height: 20),
          ],
        ),
      ),
    );
  }

  // [1] 러닝 요약 카드
  Widget _buildSummaryCard() {
    return _buildCardLayout(
      title: "러닝 요약 (Best Record)", // 타이틀 변경
      headerAction: GestureDetector(
        onTap: () {
          // 리플레이 화면 이동 (그룹 ID가 있을 때만)
          if (widget.groupId != null) {
            Navigator.push(context, MaterialPageRoute(builder: (context) => ReplayScreen(groupId: widget.groupId.toString())));
          } else {
            ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("리플레이를 재생할 수 없습니다.")));
          }
        },
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(color: const Color(0xFF2C3E50), borderRadius: BorderRadius.circular(20)),
          child: const Row(
            children: [
              Icon(Icons.play_circle_outline, color: Colors.white, size: 16),
              SizedBox(width: 4),
              Text("Replay", style: TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold)),
            ],
          ),
        ),
      ),
      child: Column(
        children: [
          Text(_recordData['runTime'] ?? "00:00", style: const TextStyle(fontSize: 40, fontWeight: FontWeight.w900, color: Colors.black87)),
          const Text("총 소요 시간", style: TextStyle(color: Colors.grey, fontSize: 12)),
          const SizedBox(height: 24),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              _buildSummaryItem(_recordData['date']?.toString() ?? "-", "날짜"),
              _buildSummaryItem(_recordData['startTime']?.toString() ?? "-", "시작 시간"),
              _buildSummaryItem("${_recordData['distance']} km", "총 거리"),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              _buildSummaryItem(_recordData['avgPace']?.toString() ?? "-", "평균 페이스"),
              _buildSummaryItem("${_recordData['heartRate']} bpm", "평균 심박수"),
              _buildSummaryItem("${_recordData['calories']} kcal", "칼로리"),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSummaryItem(String value, String label) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(value, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
        Text(label, style: const TextStyle(color: Colors.grey, fontSize: 11)),
      ],
    );
  }

  // [2] 구간별 기록 테이블
  Widget _buildLapTableCard(List<dynamic> sections) {
    return _buildCardLayout(
      title: "구간별 기록",
      child: Table(
        columnWidths: const {0: FlexColumnWidth(1), 1: FlexColumnWidth(1.5)},
        children: [
          const TableRow(children: [
            Padding(padding: EdgeInsets.only(bottom: 8), child: Text("구간 (km)", style: TextStyle(color: Colors.grey), textAlign: TextAlign.center)),
            Padding(padding: EdgeInsets.only(bottom: 8), child: Text("페이스", style: TextStyle(color: Colors.grey), textAlign: TextAlign.center)),
          ]),
          ...sections.map((sec) {
            return TableRow(children: [
              Padding(padding: const EdgeInsets.symmetric(vertical: 8), child: Text("${sec['km']}km", textAlign: TextAlign.center)),
              Padding(padding: const EdgeInsets.symmetric(vertical: 8), child: Text(sec['pace'] ?? "-", textAlign: TextAlign.center)),
            ]);
          }),
        ],
      ),
    );
  }

  // [3] 페이스 그래프
  Widget _buildPaceGraphCard(List<dynamic> sections) {
    List<FlSpot> spots = [];
    double minY = 100.0;
    double maxY = 0.0;

    for (var sec in sections) {
      double x = double.tryParse(sec['km'].toString()) ?? 0;
      double y = _parsePaceToDouble(sec['pace']);
      if (y > 0) {
        spots.add(FlSpot(x, y));
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
      }
    }

    if (spots.isEmpty) return const SizedBox();

    minY = (minY - 1).clamp(0, 100);
    maxY = maxY + 1;

    return _buildCardLayout(
      title: "페이스 그래프",
      child: Column(
        children: [
          const SizedBox(height: 10),
          SizedBox(
            height: 200,
            child: LineChart(
              LineChartData(
                gridData: FlGridData(show: true, drawVerticalLine: false),
                titlesData: FlTitlesData(
                  leftTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, reservedSize: 40, getTitlesWidget: (v, m) => Text("${v.toInt()}분", style: const TextStyle(fontSize: 10)))),
                  bottomTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, getTitlesWidget: (v, m) => Text("${v.toInt()}km", style: const TextStyle(fontSize: 10)))),
                  topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                  rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
                ),
                borderData: FlBorderData(show: false),
                lineBarsData: [
                  LineChartBarData(
                    spots: spots,
                    isCurved: true,
                    color: Colors.lightGreen,
                    barWidth: 3,
                    dotData: const FlDotData(show: true),
                  ),
                ],
                minY: minY,
                maxY: maxY,
              ),
            ),
          ),
        ],
      ),
    );
  }

  // [4] 비교 카드
  Widget _buildComparisonCard() {
    String myPaceStr = _recordData['avgPace']?.toString() ?? "0'00''";
    String groupPaceStr = _recordData['groupAvgPace']?.toString() ?? "0'00''";

    double mySeconds = _parsePaceToDouble(myPaceStr) * 60;
    double groupSeconds = _parsePaceToDouble(groupPaceStr) * 60;

    double maxSeconds = (mySeconds > groupSeconds ? mySeconds : groupSeconds);
    if (maxSeconds == 0) maxSeconds = 1;

    double myRatio = (mySeconds / maxSeconds);
    double groupRatio = (groupSeconds / maxSeconds);

    if (mySeconds > 0 && myRatio < 0.2) myRatio = 0.2;
    if (groupSeconds > 0 && groupRatio < 0.2) groupRatio = 0.2;

    return _buildCardLayout(
      title: "대회 비교 기록",
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
              child: Text(
                  "실시간 내 순위: ${_recordData['myRank'] ?? 0}위 / ${_recordData['totalRunners'] ?? _recordData['totalRunner'] ?? 0}명",
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)
              )
          ),
          const SizedBox(height: 20),
          _buildBarChartRow("참가자 평균", groupRatio, Colors.grey[300]!, groupPaceStr),
          const SizedBox(height: 10),
          _buildBarChartRow("내 페이스", myRatio, primaryColor, myPaceStr),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerRight,
            child: Text(
                "→ ${_recordData['paceDifference'] ?? '-'}",
                style: const TextStyle(color: Colors.grey, fontSize: 12)
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBarChartRow(String label, double ratio, Color color, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
                flex: (ratio * 100).toInt(),
                child: Container(
                  height: 30,
                  decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(4)),
                  alignment: Alignment.centerRight,
                  padding: const EdgeInsets.only(right: 8),
                )
            ),
            Expanded(
                flex: 100 - (ratio * 100).toInt(),
                child: const SizedBox()
            ),
            const SizedBox(width: 10),
            Text(value, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
          ],
        ),
        const SizedBox(height: 4),
        Text(label, style: const TextStyle(color: Colors.grey, fontSize: 12)),
      ],
    );
  }

  // [5] 분석 카드
  Widget _buildAnalysisCard() {
    return _buildCardLayout(
      title: "러닝 분석 요약",
      child: Text(_recordData['analysisResult'] ?? "분석 데이터 없음", style: TextStyle(color: Colors.grey[700], height: 1.5)),
    );
  }

  // [6] 배지 카드
  Widget _buildBadgeCard(List<dynamic> badges) {
    return _buildCardLayout(
      title: "획득한 배지",
      child: badges.isEmpty
          ? const Text("획득한 배지가 없습니다.", style: TextStyle(color: Colors.grey))
          : Column(
        children: badges.map((badgeName) {
          return Container(
            margin: const EdgeInsets.only(bottom: 10),
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(color: Colors.grey[100], borderRadius: BorderRadius.circular(30)),
            child: Row(
              children: [
                const Icon(Icons.verified, color: Colors.orangeAccent),
                const SizedBox(width: 12),
                Text(badgeName.toString(), style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
              ],
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildCardLayout({required String title, required Widget child, Widget? headerAction}) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(20), boxShadow: [BoxShadow(color: Colors.grey.withOpacity(0.1), blurRadius: 10, spreadRadius: 2)]),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [Container(padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4), decoration: BoxDecoration(color: primaryColor, borderRadius: BorderRadius.circular(20)), child: Text(title, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold))), if (headerAction != null) headerAction]),
        const SizedBox(height: 20),
        child,
      ]),
    );
  }
}