package com.example.seolab.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {

	@NotBlank(message = "이메일은 필수입니다.")
	@Pattern(
		regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
		message = "올바른 이메일 형식이 아닙니다."
	)
	private String email;

	@NotBlank(message = "비밀번호는 필수입니다.")
	@Pattern(
		regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
		message = "올바른 비밀번호 형식이 아닙니다. (최소 8-20자, 대소문자, 숫자, 특수문자 각 1개 이상)"
	)
	private String password;
}
