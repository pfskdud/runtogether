import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../constants.dart'; // primaryColor 사용

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  // 스위치 상태 변수 (기본값 true)
  bool _marketingNoti = true;
  bool _activityNoti = true;
  bool _aiCoachVoice = true;

  @override
  void initState() {
    super.initState();
    _loadSettings(); // 저장된 설정 불러오기
  }

  // [기능] 저장된 설정 불러오기
  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _marketingNoti = prefs.getBool('marketing_noti') ?? true;
      _activityNoti = prefs.getBool('activity_noti') ?? true;
      _aiCoachVoice = prefs.getBool('ai_coach_voice') ?? true;
    });
  }

  // [기능] 설정 변경 및 저장
  Future<void> _updateSetting(String key, bool value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(key, value); // 내부 저장소에 저장
    setState(() {
      if (key == 'marketing_noti') _marketingNoti = value;
      if (key == 'activity_noti') _activityNoti = value;
      if (key == 'ai_coach_voice') _aiCoachVoice = value;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        title: const Text("환경 설정", style: TextStyle(color: Colors.black, fontSize: 18, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.white,
        elevation: 0,
        iconTheme: const IconThemeData(color: Colors.black),
      ),
      body: ListView(
        children: [
          // 1. 알림 설정 섹션
          _buildSectionHeader("알림 설정"),
          _buildSwitchTile(
            "마케팅 정보 수신 동의",
            "이벤트 및 혜택 소식을 받습니다.",
            _marketingNoti,
                (val) => _updateSetting('marketing_noti', val),
          ),
          _buildSwitchTile(
            "활동 및 응원 알림",
            "친구의 응원, 랭킹 변동 알림을 받습니다.",
            _activityNoti,
                (val) => _updateSetting('activity_noti', val),
          ),

          const Divider(height: 30, thickness: 8, color: Color(0xFFF5F5F5)), // 굵은 구분선

          // 2. 러닝 설정 섹션
          _buildSectionHeader("러닝 설정"),
          _buildSwitchTile(
            "AI 코치 음성 안내",
            "러닝 시작 시 자동으로 음성 안내를 켭니다.",
            _aiCoachVoice,
                (val) => _updateSetting('ai_coach_voice', val),
          ),

          const Divider(height: 30, thickness: 8, color: Color(0xFFF5F5F5)),

          // 3. 앱 정보 섹션
          _buildSectionHeader("앱 정보"),
          _buildListTile("이용약관", onTap: () => _showPrepareMsg(context)),
          _buildListTile("개인정보 처리방침", onTap: () => _showPrepareMsg(context)),
          _buildListTile("오픈소스 라이선스", onTap: () => _showPrepareMsg(context)),
          ListTile(
            contentPadding: const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
            title: const Text("앱 버전", style: TextStyle(fontSize: 16)),
            trailing: const Text("v1.0.0", style: TextStyle(color: primaryColor, fontWeight: FontWeight.bold, fontSize: 15)),
          ),

          const SizedBox(height: 40),
        ],
      ),
    );
  }

  // [위젯] 섹션 제목 (회색 작은 글씨)
  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 24, 24, 8),
      child: Text(title, style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: Colors.grey[500])),
    );
  }

  // [위젯] 스위치 토글 메뉴
  Widget _buildSwitchTile(String title, String subtitle, bool value, Function(bool) onChanged) {
    return SwitchListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
      title: Text(title, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w500)),
      subtitle: Text(subtitle, style: TextStyle(fontSize: 12, color: Colors.grey[500])),
      value: value,
      onChanged: onChanged,
      activeColor: Colors.white,
      activeTrackColor: primaryColor, // 스위치 배경 색 (켜짐)
      inactiveThumbColor: Colors.white,
      inactiveTrackColor: Colors.grey[300],
    );
  }

  // [위젯] 일반 화살표 메뉴
  Widget _buildListTile(String title, {required VoidCallback onTap}) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 24, vertical: 4),
      title: Text(title, style: const TextStyle(fontSize: 16)),
      trailing: const Icon(Icons.arrow_forward_ios, size: 16, color: Colors.grey),
      onTap: onTap,
    );
  }

  void _showPrepareMsg(BuildContext context) {
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("준비 중인 페이지입니다.")));
  }
}