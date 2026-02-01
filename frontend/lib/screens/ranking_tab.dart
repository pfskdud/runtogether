import 'dart:async';
import 'package:flutter/material.dart';
import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../constants.dart'; // baseUrl이 여기에 있다고 가정

// 랭킹 유저 모델
class RankingUser {
  final int rank;
  final String nickname;
  final String? profileImage;
  final String recordValue;
  final bool isMe;

  RankingUser({
    required this.rank,
    required this.nickname,
    this.profileImage,
    required this.recordValue,
    required this.isMe,
  });

  factory RankingUser.fromJson(Map<String, dynamic> json) {
    String? rawUrl = json['profileImage'];
    String? fullUrl;

    if (rawUrl != null && rawUrl.isNotEmpty) {
      if (rawUrl.startsWith('http')) {
        fullUrl = rawUrl;
      } else {
        fullUrl = '$baseUrl$rawUrl';
      }
    }

    return RankingUser(
      rank: json['rank'] ?? 0,
      nickname: json['nickname'] ?? '알 수 없음',
      profileImage: fullUrl,
      recordValue: json['recordValue'] ?? '-',
      isMe: json['isMe'] ?? false,
    );
  }
}

class RankingTab extends StatefulWidget {
  final int groupId;  // ★ [추가] 그룹 ID 필요
  final int courseId; // (필요 없다면 나중에 제거 가능하지만 일단 유지)

  const RankingTab({
    super.key,
    required this.groupId, // ★ 생성자 추가
    required this.courseId
  });

  @override
  State<RankingTab> createState() => _RankingTabState();
}

class _RankingTabState extends State<RankingTab> {
  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.only(top: 20, bottom: 50),
      child: Column(
        children: [
          RankingCard(
            groupId: widget.groupId, // ★ 전달
            courseId: widget.courseId,
            title: "시간순",
            type: "TOTAL",
            cardIcon: Icons.timer_outlined,
          ),
          const SizedBox(height: 20),
          RankingCard(
            groupId: widget.groupId, // ★ 전달
            courseId: widget.courseId,
            title: "구간순",
            type: "SECTION",
            isSection: true,
          ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------
// 개별 랭킹 카드 위젯
// ---------------------------------------------------------
class RankingCard extends StatefulWidget {
  final int groupId; // ★ [추가]
  final int courseId;
  final String title;
  final String type;
  final bool isSection;
  final IconData? cardIcon;

  const RankingCard({
    super.key,
    required this.groupId, // ★ 생성자 추가
    required this.courseId,
    required this.title,
    required this.type,
    this.isSection = false,
    this.cardIcon,
  });

  @override
  State<RankingCard> createState() => _RankingCardState();
}

class _RankingCardState extends State<RankingCard> {
  List<RankingUser> _users = [];
  bool _isLoading = true;
  Timer? _timer;

  int _selectedKm = 1;
  final int _maxKm = 10;

  @override
  void initState() {
    super.initState();
    _fetchData();
    _timer = Timer.periodic(const Duration(seconds: 10), (timer) {
      _fetchData(isRefresh: true);
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _fetchData({bool isRefresh = false}) async {
    if (!isRefresh && mounted) setState(() => _isLoading = true);

    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('accessToken');
      final email = prefs.getString('email') ?? '';
      final dio = Dio();

      final options = Options(headers: {
        'ngrok-skip-browser-warning': 'true',
        'Authorization': 'Bearer $token',
      });

      String query = "?email=$email&type=${widget.type}";
      if (widget.isSection) {
        query += "&km=$_selectedKm";
      }

      // ★★★ [핵심 수정] URL 변경: courses -> groups, courseId -> groupId ★★★
      // 기존: '$rankingBaseUrl/${widget.courseId}/rankings$query'
      // 변경: '$baseUrl/api/v1/groups/${widget.groupId}/rankings$query'

      final url = '$baseUrl/api/v1/groups/${widget.groupId}/rankings$query';

      final response = await dio.get(url, options: options);

      if (response.statusCode == 200 && mounted) {
        List<dynamic> list = response.data;
        setState(() {
          _users = list.map((e) => RankingUser.fromJson(e)).toList();
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _users = [];
          _isLoading = false;
        });
      }
      print("랭킹 로드 실패: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    RankingUser? rank1 = _users.length >= 1 ? _users[0] : null;
    RankingUser? rank2 = _users.length >= 2 ? _users[1] : null;
    RankingUser? rank3 = _users.length >= 3 ? _users[2] : null;
    List<RankingUser> rest = _users.length > 3 ? _users.sublist(3) : [];

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16),
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.grey.withOpacity(0.1),
            spreadRadius: 2,
            blurRadius: 10,
            offset: const Offset(0, 5),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 1. 헤더
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                decoration: BoxDecoration(
                  color: primaryColor,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  widget.title,
                  style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                ),
              ),
              if (widget.isSection)
                _buildRankDropdown(),
            ],
          ),

          const SizedBox(height: 30),

          // 2. 데이터 표시
          if (_isLoading)
            const Padding(
              padding: EdgeInsets.all(20.0),
              child: Center(child: CircularProgressIndicator(color: primaryColor)),
            )
          else
            Column(
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    _buildPodiumUser(rank2, 2),
                    _buildPodiumUser(rank1, 1),
                    _buildPodiumUser(rank3, 3),
                  ],
                ),
                const SizedBox(height: 20),
                const Divider(),

                if (rest.isEmpty && rank1 != null)
                  const Padding(
                    padding: EdgeInsets.all(20),
                    child: Text("다음 순위 도전!", style: TextStyle(color: Colors.grey)),
                  )
                else if (rank1 == null)
                  const Padding(
                    padding: EdgeInsets.all(20),
                    child: Text("아직 기록이 없습니다.\n첫 번째 주인공이 되어보세요!", textAlign: TextAlign.center, style: TextStyle(color: Colors.grey)),
                  )
                else
                  ListView.builder(
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    itemCount: rest.length,
                    itemBuilder: (context, index) {
                      return _buildListRow(rest[index]);
                    },
                  ),
              ],
            ),
        ],
      ),
    );
  }

  Widget _buildRankDropdown() {
    return PopupMenuButton<int>(
      initialValue: _selectedKm,
      onSelected: (int value) {
        setState(() => _selectedKm = value);
        _fetchData();
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: Colors.grey.shade300),
          boxShadow: [
            BoxShadow(
              color: Colors.grey.withOpacity(0.1),
              blurRadius: 4,
              offset: const Offset(0, 2),
            )
          ],
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text("${_selectedKm}km", style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: Colors.black87)),
            const SizedBox(width: 4),
            const Icon(Icons.keyboard_arrow_down_rounded, size: 16, color: Colors.grey),
          ],
        ),
      ),
      color: Colors.white,
      elevation: 4,
      offset: const Offset(0, 40),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      itemBuilder: (context) {
        return List.generate(_maxKm, (index) => index + 1).map((km) {
          final isSelected = km == _selectedKm;
          return PopupMenuItem<int>(
            value: km,
            height: 40,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  "${km}km",
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                    color: isSelected ? primaryColor : Colors.black87,
                  ),
                ),
                if (isSelected)
                  const Icon(Icons.check_circle_rounded, color: primaryColor, size: 18),
              ],
            ),
          );
        }).toList();
      },
    );
  }

  Widget _buildPodiumUser(RankingUser? user, int rank) {
    final double size = rank == 1 ? 90 : 70;
    Color badgeColor;
    if (rank == 1) badgeColor = const Color(0xFFFF7E36);
    else if (rank == 2) badgeColor = Colors.grey;
    else badgeColor = const Color(0xFFCD7F32);

    return Column(
      children: [
        Stack(
          children: [
            Container(
              width: size,
              height: size,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: Colors.grey[200],
                border: user?.isMe == true ? Border.all(color: primaryColor, width: 2) : null,
                image: (user?.profileImage != null)
                    ? DecorationImage(
                  image: NetworkImage(user!.profileImage!),
                  fit: BoxFit.cover,
                )
                    : null,
              ),
              child: (user?.profileImage == null)
                  ? Icon(Icons.person, color: Colors.white, size: size * 0.5)
                  : null,
            ),
            Positioned(
              top: 0,
              right: 0,
              child: Container(
                width: 24,
                height: 24,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: badgeColor,
                  shape: BoxShape.circle,
                  border: Border.all(color: Colors.white, width: 2),
                ),
                child: Text(
                  "$rank",
                  style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold),
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Text(
          user?.nickname ?? 'xxx',
          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
          overflow: TextOverflow.ellipsis,
        ),
        if (user != null)
          Text(
            user!.recordValue,
            style: const TextStyle(fontSize: 12, color: Colors.grey),
          ),
      ],
    );
  }

  Widget _buildListRow(RankingUser user) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12),
      color: user.isMe ? primaryColor.withOpacity(0.05) : Colors.transparent,
      child: Row(
        children: [
          SizedBox(
            width: 30,
            child: Text(
              "${user.rank}",
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, fontStyle: FontStyle.italic),
              textAlign: TextAlign.center,
            ),
          ),
          const SizedBox(width: 10),
          CircleAvatar(
            radius: 20,
            backgroundColor: Colors.grey[200],
            backgroundImage: user.profileImage != null ? NetworkImage(user.profileImage!) : null,
            child: user.profileImage == null ? const Icon(Icons.person, color: Colors.grey) : null,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              user.nickname,
              style: TextStyle(fontSize: 16, fontWeight: user.isMe ? FontWeight.bold : FontWeight.normal),
            ),
          ),
          Text(
            user.recordValue,
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
          ),
        ],
      ),
    );
  }
}