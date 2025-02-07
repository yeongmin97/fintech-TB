package mesbiens.member.service;

import mesbiens.member.dto.MemberDTO;
import mesbiens.member.dto.MemberResponseDTO;
import mesbiens.member.repository.MemberRepository;
import mesbiens.member.vo.MemberVO;
import mesbiens.security.JwtTokenProvider;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final LoginRecordService loginRecordService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
        public MemberService(
            MemberRepository memberRepository,
            LoginRecordService loginRecordService,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
        ) {
            this.memberRepository = memberRepository;
            this.loginRecordService = loginRecordService;
            this.passwordEncoder = passwordEncoder;
            this.jwtTokenProvider = jwtTokenProvider;
        }
        
        //  사용자 등록 (회원가입)
        public MemberResponseDTO register(MemberDTO memberDTO) {
            if (memberRepository.findByMemberEmail(memberDTO.getMemberEmail()).isPresent() ||
                memberRepository.findByMemberId(memberDTO.getMemberId()).isPresent()) {
                throw new IllegalArgumentException("이미 존재하는 이메일 또는 ID입니다.");
            }

            if (memberDTO.getMemberPassword() == null || memberDTO.getMemberPassword().isEmpty()) {
                throw new IllegalArgumentException("비밀번호는 필수 입력값입니다.");
            }

            MemberVO member = new MemberVO();
            member.setMemberName(memberDTO.getMemberName());
            member.setMemberEmail(memberDTO.getMemberEmail());
            member.setMemberId(memberDTO.getMemberId());
            member.setMemberPassword(passwordEncoder.encode(memberDTO.getMemberPassword()));
            member.setMemberPhone(memberDTO.getMemberPhone());
            member.setMemberAddress(memberDTO.getMemberAddress());
            member.setMemberBirth(memberDTO.getMemberBirth());
            member.setMemberSnsSignUpYN("N");

            MemberVO savedMember = memberRepository.save(member);

            return new MemberResponseDTO(savedMember);
        }
        
        //  로그인 (Access Token + Refresh Token 발급)
        public void login(String memberId, String password, String requestIp, HttpServletResponse response) {
            if (memberId == null || memberId.trim().isEmpty()) {
                throw new IllegalArgumentException("아이디를 입력해주세요.");
            }

            MemberVO member = memberRepository.findByMemberId(memberId)
                    .orElseThrow(() -> {
                        loginRecordService.saveLoginRecord(null, requestIp, "F", "존재하지 않는 사용자 ID");
                        return new IllegalArgumentException("존재하지 않는 사용자 ID입니다.");
                    });

            if (!passwordEncoder.matches(password, member.getMemberPassword())) {
                loginRecordService.saveLoginRecord(member, requestIp, "F", "비밀번호가 일치하지 않습니다.");
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }

            loginRecordService.saveLoginRecord(member, requestIp, "S", null);

            //  Access Token 및 Refresh Token 생성
            String accessToken = jwtTokenProvider.createAccessToken(member.getMemberId(), "USER_ROLE");
            String refreshToken = jwtTokenProvider.createRefreshToken(member.getMemberId());

            //  쿠키에 JWT 저장
            jwtTokenProvider.addJwtTokenToCookie(response, accessToken);
            jwtTokenProvider.addRefreshTokenToCookie(response, refreshToken);
        }

        //  로그아웃 (쿠키 삭제)
        public void logout(HttpServletResponse response) {
            jwtTokenProvider.removeJwtTokenFromCookie(response);
            jwtTokenProvider.removeRefreshTokenFromCookie(response);
        }

        //  사용자 정보 조회 (JWT 인증 필요)
        public MemberResponseDTO findById(String memberId) {
            MemberVO member = memberRepository.findByMemberId(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
            return new MemberResponseDTO(member);
        }

        //  회원 인증 (아이디 및 비밀번호 확인)
        public boolean authenticateUser(String memberId, String password) {
            if (memberId == null || memberId.isEmpty()) {
                throw new IllegalArgumentException("아이디를 입력해주세요.");
            }

            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("비밀번호를 입력해주세요.");
            }

            Optional<MemberVO> member = memberRepository.findByMemberId(memberId);
            return member.isPresent() && passwordEncoder.matches(password, member.get().getMemberPassword());
        }

        //  사용자 ID로 찾기
        public Optional<MemberVO> findByMemberId(String memberId) {
            return memberRepository.findByMemberId(memberId);
        }
    }
