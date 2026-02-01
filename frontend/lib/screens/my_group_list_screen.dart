import 'package:flutter/material.dart';
import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../constants.dart';

import 'package:runtogether_team04/screens/group_detail_screen.dart';
import 'package:runtogether_team04/screens/my_record_screen.dart';
import 'package:runtogether_team04/screens/ranking_tab.dart';

// 모드 4가지로 확장 (general 추가됨)
// general: 기본 입장 모드 (하단 탭용)
// management: 삭제/탈퇴 모드 (마이페이지용)
// record: 기록 조회 모드
// ranking: 랭킹 조회 모드
enum GroupListMode { general, management, record, ranking }

class MyGroupListScreen extends StatefulWidget {
  final GroupListMode mode;

  const MyGroupListScreen({
    super.key,
    required this.mode,
  });

  @override
  State<MyGroupListScreen> createState() => _MyGroupListScreenState();
}

class _MyGroupListScreenState extends State<MyGroupListScreen> {
  List<dynamic> _myGroups = [];
  bool _isLoading = true;
  String _errorMessage = "";

  @override
  void initState() {
    super.initState();
    _fetchMyGroups();
  }

  Future<void> _fetchMyGroups() async {
    if (!mounted) return;
    setState(() { _isLoading = true; _errorMessage = ""; });
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('accessToken');
      if (token == null) {
        if (mounted) setState(() => _isLoading = false);
        return;
      }
      final dio = Dio();
      final response = await dio.get(myGroupUrl, options: Options(headers: {'ngrok-skip-browser-warning': 'true', 'Authorization': 'Bearer $token'}));
      if (response.statusCode == 200) {
        if (mounted) {
          setState(() {
            if (response.data is List) _myGroups = response.data;
            else if (response.data is Map && response.data['result'] is List) _myGroups = response.data['result'];
            else _myGroups = [];
          });
        }
      }
    } catch (e) {
      if (mounted) setState(() => _errorMessage = "데이터를 불러올 수 없습니다.");
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _leaveGroup(int groupId, int index, bool isOwner) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('accessToken');
      final dio = Dio();
      Response response;
      if (isOwner) {
        response = await dio.delete('$baseUrl/api/v1/groups/$groupId', options: Options(headers: {'Authorization': 'Bearer $token', 'ngrok-skip-browser-warning': 'true'}));
      } else {
        response = await dio.delete('$baseUrl/api/v1/groups/$groupId/leave', options: Options(headers: {'Authorization': 'Bearer $token', 'ngrok-skip-browser-warning': 'true'}));
      }
      if (response.statusCode == 200) {
        setState(() => _myGroups.removeAt(index));
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(isOwner ? "대회가 삭제되었습니다." : "대회 참가가 취소되었습니다.")));
      }
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("오류가 발생했습니다.")));
    }
  }

  void _showLeaveDialog(int groupId, int index, String groupName, bool isOwner) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        icon: Container(margin: const EdgeInsets.only(top: 8), width: 70, height: 70, decoration: BoxDecoration(color: Colors.red.withOpacity(0.1), shape: BoxShape.circle), child: const Icon(Icons.warning_rounded, size: 36, color: Colors.redAccent)),
        title: Text(isOwner ? "대회 삭제" : "대회 탈퇴", style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 20)),
        content: Text(isOwner ? "'$groupName' 대회를 삭제하시겠습니까?" : "'$groupName' 대회를 나가시겠습니까?", textAlign: TextAlign.center),
        actions: [
          Row(children: [
            Expanded(child: TextButton(onPressed: () => Navigator.pop(ctx), child: const Text("취소", style: TextStyle(color: Colors.grey)))),
            Expanded(child: TextButton(onPressed: () { Navigator.pop(ctx); _leaveGroup(groupId, index, isOwner); }, child: Text(isOwner ? "삭제" : "탈퇴", style: const TextStyle(color: Colors.redAccent, fontWeight: FontWeight.bold)))),
          ])
        ],
      ),
    );
  }

  String _getTitle() {
    switch (widget.mode) {
      case GroupListMode.general: return "내 대회 목록"; // 원래 UI용
      case GroupListMode.management: return "나의 대회 관리";
      case GroupListMode.record: return "러닝 기록 - 대회 선택";
      case GroupListMode.ranking: return "랭킹 - 대회 선택";
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: AppBar(
        backgroundColor: primaryColor,
        elevation: 0,
        centerTitle: true,
        title: Text(_getTitle(), style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
        // 관리 모드가 아니고 메인 탭이 아닐 때(네비게이션 스택이 있을 때)만 뒤로가기
        leading: (widget.mode != GroupListMode.general && Navigator.canPop(context))
            ? IconButton(icon: const Icon(Icons.arrow_back, color: Colors.white), onPressed: () => Navigator.pop(context))
            : null,
        automaticallyImplyLeading: false, // 탭바에서 뒤로가기 자동 생성 방지
      ),
      body: _isLoading ? const Center(child: CircularProgressIndicator(color: primaryColor))
          : _errorMessage.isNotEmpty ? Center(child: Text(_errorMessage))
          : _myGroups.isEmpty ? const Center(child: Text('참여 중인 대회가 없습니다.'))
          : ListView.separated(
        padding: const EdgeInsets.all(20),
        itemCount: _myGroups.length,
        separatorBuilder: (ctx, i) => const SizedBox(height: 16),
        itemBuilder: (ctx, i) => _buildMyGroupCard(_myGroups[i], i),
      ),
    );
  }

  Widget _buildMyGroupCard(dynamic group, int index) {
    int finalId = group['id'] ?? group['groupId'] ?? 0;
    int courseId = group['courseId'] ?? 0;
    String groupName = group['groupName'] ?? group['title'] ?? '제목 없음';
    print("🧐 [리스트 데이터 확인] $index번방: 이름=$groupName, ID=$finalId, 원본=$group");
    String description = group['description'] ?? '설명이 없습니다.';
    int count = group['currentPeople'] ?? 0;
    bool isOwner = (group['isOwner'] == true) || (group['owner'] == true);
    List<String> tags = group['tags'] != null ? group['tags'].toString().split(' ').where((t) => t.isNotEmpty).toList() : [];

    // ★ 아이콘 설정 (general 모드는 기본 아이콘)
    IconData iconData;
    Color iconColor;

    switch (widget.mode) {
      case GroupListMode.general: // 기본 입장 모드
        iconData = Icons.directions_run; iconColor = primaryColor; break;
      case GroupListMode.management:
        iconData = Icons.settings; iconColor = Colors.grey; break;
      case GroupListMode.record:
        iconData = Icons.history_edu; iconColor = primaryColor; break;
      case GroupListMode.ranking:
        iconData = Icons.emoji_events; iconColor = Colors.amber; break;
    }

    return GestureDetector(
      onTap: () {
        print("👉 [클릭 확인] '$groupName' 클릭! (ID: $finalId) 로 이동 시도");

        // ★ general(입장) 모드와 management(관리) 모드는 클릭 시 상세 화면으로 이동
        if (widget.mode == GroupListMode.general || widget.mode == GroupListMode.management) {
          if (finalId != 0) Navigator.push(context, MaterialPageRoute(builder: (context) => GroupDetailScreen(groupId: finalId, groupName: groupName)));
        }
        else if (widget.mode == GroupListMode.record) {
          Navigator.push(context, MaterialPageRoute(builder: (context) => Scaffold(
            appBar: AppBar(title: Text("$groupName 기록"), backgroundColor: Colors.white, foregroundColor: Colors.black, elevation: 0),
            body: const MyRecordScreen(isEmbedded: false),
          )));
        }
        else if (widget.mode == GroupListMode.ranking) {
          print("🚀 랭킹 요청! 클릭한 그룹 ID: $finalId / 이름: $groupName");
          Navigator.push(context, MaterialPageRoute(builder: (context) => Scaffold(
            appBar: AppBar(title: Text("$groupName 랭킹"), backgroundColor: Colors.white, foregroundColor: Colors.black, elevation: 0),
            body: RankingTab(
            groupId: finalId,
            courseId: courseId,
          ),
          )));
        }
      },
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(20), boxShadow: [BoxShadow(color: Colors.grey.withOpacity(0.1), spreadRadius: 1, blurRadius: 10, offset: const Offset(0, 5))]),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                // 아이콘 + 제목 (일반 모드는 아이콘 없이 제목만 크게)
                Expanded(
                  child: Row(
                    children: [
                      // 관리, 기록, 랭킹 모드일 때만 아이콘 표시
                      if (widget.mode != GroupListMode.general) ...[
                        Container(width: 40, height: 40, decoration: BoxDecoration(color: iconColor.withOpacity(0.1), borderRadius: BorderRadius.circular(10)), child: Icon(iconData, color: iconColor, size: 20)),
                        const SizedBox(width: 12),
                      ],
                      Expanded(child: Text(groupName, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18), overflow: TextOverflow.ellipsis)),
                    ],
                  ),
                ),

                // ★ 버튼 분기 처리
                if (widget.mode == GroupListMode.management)
                // 1. 관리 모드: 삭제/탈퇴 버튼 (빨간 테두리)
                  OutlinedButton(
                    onPressed: () => _showLeaveDialog(finalId, index, groupName, isOwner),
                    style: OutlinedButton.styleFrom(minimumSize: const Size(60, 32), padding: const EdgeInsets.symmetric(horizontal: 12), side: const BorderSide(color: Colors.redAccent), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20))),
                    child: Text(isOwner ? "삭제" : "탈퇴", style: const TextStyle(color: Colors.redAccent, fontSize: 12, fontWeight: FontWeight.bold)),
                  )
                else if (widget.mode == GroupListMode.general)
                // 2. ★ 일반(입장) 모드: 원래 UI인 "입장" 버튼 (주황색 채워짐)
                  ElevatedButton(
                    onPressed: () { if (finalId != 0) Navigator.push(context, MaterialPageRoute(builder: (context) => GroupDetailScreen(groupId: finalId, groupName: groupName))); },
                    style: ElevatedButton.styleFrom(backgroundColor: primaryColor, minimumSize: const Size(60, 32), padding: const EdgeInsets.symmetric(horizontal: 12), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20))),
                    child: const Text("입장", style: TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.bold)),
                  )
                else
                // 3. 기록/랭킹 모드: 화살표 아이콘
                  const Icon(Icons.arrow_forward_ios, size: 16, color: Colors.grey),
              ],
            ),
            const SizedBox(height: 12),
            if (tags.isNotEmpty) Wrap(spacing: 8, children: tags.map((t) => Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4), decoration: BoxDecoration(color: Colors.grey[200], borderRadius: BorderRadius.circular(6)), child: Text(t, style: TextStyle(color: Colors.grey[700], fontSize: 11)))).toList()),
            const SizedBox(height: 12),
            const Divider(height: 1, thickness: 1, color: Color(0xFFEEEEEE)),
            const SizedBox(height: 12),
            Text(description, style: TextStyle(color: Colors.grey[600], fontSize: 14), maxLines: 1, overflow: TextOverflow.ellipsis),
            const SizedBox(height: 12),
            Text("$count명 참여 중", style: const TextStyle(color: Colors.redAccent, fontWeight: FontWeight.bold, fontSize: 13)),
          ],
        ),
      ),
    );
  }
}