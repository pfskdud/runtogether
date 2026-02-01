import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../constants.dart';
import 'main_screen.dart';

class ProfileSetupScreen extends StatefulWidget {
  final bool isEditMode; // ★ 수정 모드 여부

  const ProfileSetupScreen({
    super.key,
    this.isEditMode = false, // 기본값은 '최초 설정(false)'
  });

  @override
  State<ProfileSetupScreen> createState() => _ProfileSetupScreenState();
}

class _ProfileSetupScreenState extends State<ProfileSetupScreen> {
  final _nicknameController = TextEditingController();

  String _gender = '남성';
  DateTime _birthDate = DateTime(1995, 5, 5);

  // 기존 프로필 이미지 URL (수정 모드일 때 보여주기 위함)
  String? _existingImageUrl;

  File? _profileImage;
  final ImagePicker _picker = ImagePicker();
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    // ★ 수정 모드라면, 기존 데이터를 서버에서 가져와서 채워넣기
    if (widget.isEditMode) {
      _fetchUserProfile();
    }
  }

  // [수정 모드 전용] 기존 회원 정보 불러오기
  Future<void> _fetchUserProfile() async {
    setState(() => _isLoading = true);
    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('accessToken');
      final dio = Dio();

      // 마이페이지 조회 API 사용 (정보 채우기용)
      final response = await dio.get(
        '$baseUrl/api/v1/auth/mypage',
        options: Options(headers: {
          'Authorization': 'Bearer $token',
          'ngrok-skip-browser-warning': 'true',
        }),
      );

      if (response.statusCode == 200) {
        final data = response.data;
        setState(() {
          _nicknameController.text = data['nickname'] ?? "";

          // 성별 데이터 매핑 (MALE -> 남성, FEMALE -> 여성)
          String serverGender = data['gender'] ?? "MALE";
          _gender = (serverGender == "FEMALE") ? "여성" : "남성";

          // 생년월일 파싱 (yyyy-MM-dd)
          if (data['birthDate'] != null) {
            try {
              _birthDate = DateTime.parse(data['birthDate']);
            } catch (e) {
              print("날짜 파싱 에러: $e");
            }
          }

          String rawUrl = data['profileImage'] ?? "";
          if (rawUrl.isNotEmpty) {
            // 1. http 없으면 baseUrl 붙이기
            String fullUrl = rawUrl.startsWith("http") ? rawUrl : "$baseUrl$rawUrl";

            // 2. 캐시 무시를 위해 시간값(?v=...) 붙이기
            _existingImageUrl = "$fullUrl?v=${DateTime.now().millisecondsSinceEpoch}";
          } else {
            _existingImageUrl = null;
          }
        });
      }
    } catch (e) {
      print("기존 정보 로드 실패: $e");
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  // 갤러리에서 이미지 선택
  Future<void> _pickImage() async {
    final XFile? image = await _picker.pickImage(source: ImageSource.gallery);
    if (image != null) setState(() => _profileImage = File(image.path));
  }

  // [프로필 저장/수정 함수] - FormData 방식으로 수정됨
  void _saveProfile() async {
    if (_nicknameController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('닉네임을 입력해주세요.')));
      return;
    }

    setState(() => _isLoading = true);

    try {
      final prefs = await SharedPreferences.getInstance();
      final token = prefs.getString('accessToken');

      if (token == null) return;

      final dio = Dio();

      // ★ 중요: FormData를 쓸 때는 Content-Type을 직접 적지 않아도 Dio가 알아서 설정해줍니다.
      final options = Options(headers: {
        'Authorization': 'Bearer $token',
        'ngrok-skip-browser-warning': 'true',
      });

      String serverGender = (_gender == '남성') ? 'MALE' : 'FEMALE';
      String birthDateStr = "${_birthDate.year}-${_birthDate.month.toString().padLeft(2,'0')}-${_birthDate.day.toString().padLeft(2,'0')}";

      // ★ [핵심 수정] Map 대신 FormData 생성
      final formData = FormData.fromMap({
        "nickname": _nicknameController.text,
        "gender": serverGender,
        "birthDate": birthDateStr,
        // 이미지가 있을 때만 파일을 첨부합니다.
        if (_profileImage != null)
          "image": await MultipartFile.fromFile(_profileImage!.path),
        // 주의: 서버가 받는 키 이름이 'profileImage'인지 'file'인지 확인 필요! 보통 'profileImage'나 'file'을 씁니다.
      });

      Response response;

      response = await dio.post(profileUrl, data: formData, options: options);

      if (response.statusCode == 200) {
        if (!mounted) return;

        if (widget.isEditMode) {
          Navigator.pop(context);
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("프로필이 수정되었습니다.")));
        } else {
          Navigator.pushAndRemoveUntil(context, MaterialPageRoute(builder: (context) => const MainScreen()), (route) => false);
        }
      }
    } catch (e) {
      print("❌ 저장 에러: $e");
      // 에러 메시지 상세 확인 (디버깅용)
      if (e is DioException) {
        print("서버 응답: ${e.response?.data}");
      }
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("저장에 실패했습니다.")));
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    // 이미지 우선순위: 1.새로 선택한 파일 -> 2.기존 네트워크 이미지 -> 3.기본 아이콘
    ImageProvider? backgroundImage;
    if (_profileImage != null) {
      backgroundImage = FileImage(_profileImage!);
    } else if (_existingImageUrl != null && _existingImageUrl!.isNotEmpty) {
      backgroundImage = NetworkImage(_existingImageUrl!);
    }

    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
          title: Text(widget.isEditMode ? '프로필 수정' : '프로필 설정'), // 제목 변경
          backgroundColor: Colors.white,
          foregroundColor: Colors.black,
          elevation: 0
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // --- 프로필 사진 ---
              Center(
                child: GestureDetector(
                  onTap: _pickImage,
                  child: Stack(
                    children: [
                      CircleAvatar(
                        radius: 50,
                        backgroundColor: Colors.grey[200],
                        backgroundImage: backgroundImage,
                        child: (backgroundImage == null) ? const Icon(Icons.person, size: 50, color: Colors.grey) : null,
                      ),
                      Positioned(
                        bottom: 0, right: 0,
                        child: Container(
                          padding: const EdgeInsets.all(4),
                          decoration: const BoxDecoration(color: Colors.white, shape: BoxShape.circle),
                          child: const Icon(Icons.camera_alt, color: Colors.grey, size: 20),
                        ),
                      )
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 30),

              // --- 닉네임 입력 ---
              const Text('닉네임', style: TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              TextField(
                  controller: _nicknameController,
                  decoration: InputDecoration(
                    hintText: '닉네임 입력',
                    filled: true,
                    fillColor: Colors.grey[100],
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                  )
              ),
              const SizedBox(height: 24),

              // --- 성별 선택 ---
              const Text('성별', style: TextStyle(fontWeight: FontWeight.bold)),
              Row(children: [_buildGenderRadio('남성'), _buildGenderRadio('여성')]),
              const SizedBox(height: 24),

              // --- 생년월일 ---
              const Text('생년월일', style: TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: 8),
              InkWell(
                onTap: () async {
                  final date = await showDatePicker(
                      context: context,
                      initialDate: _birthDate,
                      firstDate: DateTime(1900),
                      lastDate: DateTime.now()
                  );
                  if (date != null) setState(() => _birthDate = date);
                },
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                  width: double.infinity,
                  decoration: BoxDecoration(color: Colors.grey[100], borderRadius: BorderRadius.circular(12)),
                  child: Text(
                    "${_birthDate.year}-${_birthDate.month.toString().padLeft(2,'0')}-${_birthDate.day.toString().padLeft(2,'0')}",
                    style: const TextStyle(fontSize: 16),
                  ),
                ),
              ),

              const SizedBox(height: 40),

              // --- 완료 버튼 ---
              SizedBox(
                width: double.infinity,
                height: 55,
                child: _isLoading
                    ? const Center(child: CircularProgressIndicator(color: primaryColor))
                    : ElevatedButton(
                  onPressed: _saveProfile,
                  style: ElevatedButton.styleFrom(backgroundColor: primaryColor, shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12))),
                  child: Text(widget.isEditMode ? '수정 완료' : '완료', style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
                ),
              ),
              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildGenderRadio(String label) {
    return Row(
      children: [
        Radio<String>(
          value: label,
          groupValue: _gender,
          activeColor: primaryColor,
          onChanged: (val) {
            setState(() {
              _gender = val!;
            });
          },
        ),
        Text(label),
        const SizedBox(width: 20),
      ],
    );
  }
}