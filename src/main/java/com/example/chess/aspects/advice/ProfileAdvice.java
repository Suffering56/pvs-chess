package com.example.chess.aspects.advice;

import com.example.chess.aspects.Profile;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Log4j2
@Aspect
@Component
public class ProfileAdvice {

	//FIXME: not working with multithreading;
	@Around("@annotation(com.example.chess.aspects.Profile)")
	public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
		long start = System.currentTimeMillis();

		Object proceed = joinPoint.proceed();

		long executionTime = System.currentTimeMillis() - start;

		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		Profile profile = method.getAnnotation(Profile.class);

		String signatureText = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
		if (profile.showMethodArgsCount()) {
			signatureText = signatureText + "[" + +signature.getMethod().getParameterCount() + "]";
		}
		log.info("<" + signatureText + "> executed in " + executionTime + "ms");

		return proceed;
	}
}
