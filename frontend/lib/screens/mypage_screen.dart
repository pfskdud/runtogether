import 'package:flutter/material.dart';
import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../constants.dart';
import 'package:runtogether_team04/screens/profile_setup_screen.dart';
import 'package:runtogether_team04/screens/login_screen.dart';
import 'package:runtogether_team04/screens/settings_screen.dart';
import 'package:runtogether_team04/screens/my_group_list_screen.dart';

class MyPageScreen extends StatefulWidget {
  const MyPageScreen({super.key});

  @override
  State<MyPageScreen> createState() => _MyPageScreenState();
}

class _MyPageScreenState extends State<MyPageScreen> {
  bool _isLoading = true;


  String _nickname = "";
  String _userCode = "";
  String _profileImage = "";

  String _competitionTitle = "최근 기록이 없습니다.";
  String _courseName = "-";
  String _period = "-";
  String _totalDistance = "0";
  String _totalTime = "00:00:00";
  int _totalCalories = 0;

  @override
  void initState() {
    super.initState();
    _fetchMyPageData();
  }

  // [API] 마이페이지 정보 로드
  Future<void> _fetchMyPageData() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('accessToken');

      if (token == null) {
        setState(() => _isLoading = false);
        return;
      }

      final dio = Dio();
      final options = Options(headers: {
        'Authorization': 'Bearer $token',
        'ngrok-skip-browser-warning': 'true',
        'Content-Type': 'application/json',
      });

      // GET 요청 (constants.dart에 myPageUrl이 없다면 직접 주소 입력)
      final response = await dio.get('$baseUrl/api/v1/auth/mypage', options: options);

      if (response.statusCode == 200) {
        final data = response.data;
        if (mounted) {
          setState(() {
            _nickname = data['nickname'] ?? "이름 없음";
            _userCode = data['userCode'] ?? "-";
            String rawUrl = data['profileImage'] ?? "";
            if (rawUrl.isNotEmpty) {
              // 1. 전체 주소 만들기
              String fullUrl = rawUrl.startsWith("http") ? rawUrl : "$baseUrl$rawUrl";

              // ★ [수정] 뒤에 현재 시간을 붙여서 캐시를 무시하고 새로고침하게 만듭니다!
              // 예: .../image.jpg?v=123456789
              _profileImage = "$fullUrl?v=${DateTime.now().millisecondsSinceEpoch}";

            } else {
              _profileImage = "";
            }

            print("📸 [MyPage] 이미지 주소 업데이트됨: $_profileImage");

            _competitionTitle = data['competitionTitle'] ?? "참여한 대회가 없습니다.";
            _courseName = data['courseName'] ?? "-";
            _period = data['period'] ?? "-";
            _totalDistance = data['totalDistance'] ?? "0";
            _totalTime = data['totalTime'] ?? "00:00:00";
            _totalCalories = data['totalCalories'] ?? 0;
          });
        }
      }
    } catch (e) {
      print("❌ 마이페이지 로드 실패: $e");
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  // 회원탈퇴 로직
  Future<void> _deleteAccount() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('accessToken');
      if (token == null) return;

      final dio = Dio();
      final response = await dio.delete(
        '$baseUrl/api/v1/auth/withdraw',
        options: Options(headers: {
          'Authorization': 'Bearer $token',
          'ngrok-skip-browser-warning': 'true',
        }),
      );

      if (response.statusCode == 200) {
        await prefs.clear();
        if (!mounted) return;
        Navigator.pushAndRemoveUntil(context, MaterialPageRoute(builder: (context) => const LoginScreen()), (route) => false);
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("회원 탈퇴가 완료되었습니다.")));
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("오류가 발생했습니다.")));
    }
  }

  void _showDeleteDialog() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        icon: Container(margin: const EdgeInsets.only(top: 10), width: 80, height: 80, decoration: BoxDecoration(color: Colors.red.withOpacity(0.1), shape: BoxShape.circle), child: const Icon(Icons.warning_amber_rounded, size: 40, color: Colors.redAccent)),
        title: const Text("회원탈퇴", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20), textAlign: TextAlign.center),
        content: Text("정말로 탈퇴하시겠습니까?\n모든 기록이 삭제됩니다.", style: TextStyle(color: Colors.grey[600], fontSize: 14, height: 1.4), textAlign: TextAlign.center),
        actionsPadding: const EdgeInsets.fromLTRB(20, 10, 20, 20),
        actions: [
          Row(children: [
            Expanded(child: TextButton(onPressed: () => Navigator.pop(ctx), style: TextButton.styleFrom(backgroundColor: Colors.grey[200], shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)), padding: const EdgeInsets.symmetric(vertical: 14)), child: const Text("취소", style: TextStyle(color: Colors.grey, fontWeight: FontWeight.bold)))),
            const SizedBox(width: 10),
            Expanded(child: TextButton(onPressed: () { Navigator.pop(ctx); _deleteAccount(); }, style: TextButton.styleFrom(backgroundColor: Colors.redAccent, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)), padding: const EdgeInsets.symmetric(vertical: 14)), child: const Text("탈퇴", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)))),
          ])
        ],
      ),
    );
  }

  void _showLogoutDialog() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        icon: Container(margin: const EdgeInsets.only(top: 10), width: 80, height: 80, decoration: BoxDecoration(color: primaryColor.withOpacity(0.1), shape: BoxShape.circle), child: const Icon(Icons.logout_rounded, size: 40, color: primaryColor)),
        title: const Text("로그아웃", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 20), textAlign: TextAlign.center),
        content: const Text("정말 로그아웃 하시겠습니까?", style: TextStyle(fontSize: 15), textAlign: TextAlign.center),
        actionsPadding: const EdgeInsets.fromLTRB(20, 10, 20, 20),
        actions: [
          Row(children: [
            Expanded(child: TextButton(onPressed: () => Navigator.pop(ctx), style: TextButton.styleFrom(backgroundColor: Colors.grey[200], shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)), padding: const EdgeInsets.symmetric(vertical: 14)), child: const Text("취소", style: TextStyle(color: Colors.grey, fontWeight: FontWeight.bold)))),
            const SizedBox(width: 10),
            Expanded(child: TextButton(onPressed: () async { Navigator.pop(ctx); final prefs = await SharedPreferences.getInstance(); await prefs.clear(); if (!mounted) return; Navigator.pushAndRemoveUntil(context, MaterialPageRoute(builder: (context) => const LoginScreen()), (route) => false); }, style: TextButton.styleFrom(backgroundColor: primaryColor, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)), padding: const EdgeInsets.symmetric(vertical: 14)), child: const Text("로그아웃", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)))),
          ])
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) return const Scaffold(backgroundColor: Colors.white, body: Center(child: CircularProgressIndicator(color: primaryColor)));

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(title: const Text("마이페이지", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)), centerTitle: true, backgroundColor: primaryColor, elevation: 0, automaticallyImplyLeading: false),
      body: SingleChildScrollView(
        child: Column(
          children: [
            _buildProfileSection(),
            _buildRecentRaceSection(),
            const SizedBox(height: 20),

            // 메뉴 리스트
            _buildMenuItem("프로필 수정"),
            _buildDivider(),
            _buildMenuItem("나의 대회 관리"),
            _buildDivider(),
            _buildMenuItem("러닝 기록"),
            _buildDivider(),
            _buildMenuItem("배지"),
            _buildDivider(),
            _buildMenuItem("랭킹"),
            _buildDivider(),
            _buildMenuItem("환경 설정"),
            _buildDivider(),
            const SizedBox(height: 40),
            TextButton(onPressed: _showDeleteDialog, child: const Text("회원탈퇴", style: TextStyle(color: Colors.grey, fontSize: 13, decoration: TextDecoration.underline))),
            const SizedBox(height: 50),
          ],
        ),
      ),
    );
  }

  Widget _buildProfileSection() {
    return Container(
      padding: const EdgeInsets.all(24),
      child: Row(
        children: [
          Container(width: 70, height: 70, decoration: BoxDecoration(shape: BoxShape.circle, border: Border.all(color: Colors.grey[300]!), image: DecorationImage(image: _profileImage.isNotEmpty ? NetworkImage(_profileImage) : const AssetImage('assets/images/character.png') as ImageProvider, fit: BoxFit.cover))),
          const SizedBox(width: 16),
          Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(_nickname, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)), const SizedBox(height: 4), Row(children: [const Text("유저 ID  ", style: TextStyle(color: Colors.grey, fontSize: 13)), Text(_userCode, style: const TextStyle(color: primaryColor, fontWeight: FontWeight.bold, fontSize: 13))])])),
          OutlinedButton(onPressed: _showLogoutDialog, style: OutlinedButton.styleFrom(side: const BorderSide(color: Colors.grey), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)), padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 0), minimumSize: const Size(0, 32)), child: const Text("로그아웃", style: TextStyle(color: Colors.grey, fontSize: 12)))
        ],
      ),
    );
  }

  Widget _buildRecentRaceSection() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text("최근 대회", style: TextStyle(color: Colors.grey, fontSize: 14)),
          const SizedBox(height: 10),
          Container(
            width: double.infinity, padding: const EdgeInsets.all(20), decoration: BoxDecoration(color: const Color(0xFFF5F5F5), borderRadius: BorderRadius.circular(16)),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(_competitionTitle, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              const SizedBox(height: 4), Text(_courseName, style: const TextStyle(color: primaryColor, fontSize: 13)), Text(_period, style: const TextStyle(color: Colors.grey, fontSize: 12)),
              const SizedBox(height: 20),
              Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [_buildRecordItem("$_totalDistance km"), _buildRecordItem(_totalTime), _buildRecordItem("$_totalCalories kcal")]),
            ]),
          ),
        ],
      ),
    );
  }

  Widget _buildRecordItem(String text) => Text(text, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Color(0xFFFF7F50)));

  // 메뉴 리스트 아이템 (이동 로직 수정됨)
  Widget _buildMenuItem(String title) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 24, vertical: 0),
      title: Text(title, style: const TextStyle(fontSize: 16)),
      trailing: const Icon(Icons.chevron_right, color: Colors.grey, size: 20), // 화살표 추가
      onTap: () async {
        if (title == "프로필 수정") {
          // ★ [수정 후] : 갔다 와서(await) -> 즉시 새로고침(_fetchMyPageData) 실행!
          await Navigator.push(
              context,
              MaterialPageRoute(builder: (context) => const ProfileSetupScreen(isEditMode: true)
              ),
          );

          print("⏳ [MyPage] 프로필 수정 완료! 데이터 새로고침 대기 중...");
          await Future.delayed(const Duration(milliseconds: 500));

          // 3. 데이터 새로고침 실행
          print("🔄 [MyPage] 데이터 새로고침 시작!");
          await _fetchMyPageData();
        }

        // ★ 1. 나의 대회 관리 -> 관리 모드로 이동
        else if (title == "나의 대회 관리") {
          // 관리 모드 (삭제/탈퇴 버튼 나옴)
          Navigator.push(context, MaterialPageRoute(builder: (context) => const MyGroupListScreen(mode: GroupListMode.management)));
        }
        else if (title == "러닝 기록") {
          // 기록 모드 (화살표)
          Navigator.push(context, MaterialPageRoute(builder: (context) => const MyGroupListScreen(mode: GroupListMode.record)));
        }
        else if (title == "랭킹") {
          // 랭킹 모드 (화살표)
          Navigator.push(context, MaterialPageRoute(builder: (context) => const MyGroupListScreen(mode: GroupListMode.ranking)));
        }

        else if (title == "환경 설정") {
          Navigator.push(context, MaterialPageRoute(builder: (context) => const SettingsScreen()));
        }
      },
    );
  }

  Widget _buildDivider() => const Divider(height: 1, thickness: 0.5, color: Colors.grey, indent: 24, endIndent: 24);
}