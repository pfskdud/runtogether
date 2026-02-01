import 'package:flutter/material.dart';
import 'package:runtogether_team04/constants.dart';
import 'package:runtogether_team04/screens/home_screen.dart';
import 'package:runtogether_team04/screens/mypage_screen.dart';
import 'package:runtogether_team04/screens/map_screen.dart';

// ★ MyGroupListScreen과 GroupListMode(enum) 사용을 위해 import
import 'package:runtogether_team04/screens/my_group_list_screen.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({super.key});

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _selectedIndex = 0;

  // 각 탭에 보여줄 화면들
  final List<Widget> _screens = [
    const HomeScreen(),

    // ★  메인 탭은 'general'(입장 모드)로 설정 -> 입장 버튼 나옴!
    const MyGroupListScreen(mode: GroupListMode.general),

    const MapScreen(),
    const MyPageScreen(),
  ];

  void _onItemTapped(int index) {
    setState(() {
      _selectedIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _screens[_selectedIndex],
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _selectedIndex,
        onTap: _onItemTapped,
        selectedItemColor: primaryColor,
        unselectedItemColor: Colors.grey,
        type: BottomNavigationBarType.fixed,
        backgroundColor: Colors.white,
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.home), label: '홈'),
          BottomNavigationBarItem(icon: Icon(Icons.group), label: '내 대회'),
          BottomNavigationBarItem(icon: Icon(Icons.location_on), label: '지도'),
          BottomNavigationBarItem(icon: Icon(Icons.person), label: '마이'),
        ],
      ),
    );
  }
}