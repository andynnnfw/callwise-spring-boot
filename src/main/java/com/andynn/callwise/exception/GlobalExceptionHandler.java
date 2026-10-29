package com.andynn.callwise.exception;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.andynn.callwise.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(basePackages= "com.andynn.callwise.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponse> tratarStatus(ResponseStatusException ex, HttpServletRequest request){
        int status = ex.getStatusCode().value();
        ErroResponse erro = new ErroResponse(status, ex.getReason(), request.getRequestURI());
        return ResponseEntity.status(status).body(erro);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mensagem = ex.getBindingResult().getFieldError().getDefaultMessage();
        ErroResponse erro = new ErroResponse(400, mensagem, request.getRequestURI());
        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoInvalido(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String mensagem = "Corpo da requisição inválido. Confira o JSON e se o tipo é NORMAL ou PRIORITARIA";
        ErroResponse erro = new ErroResponse(400, mensagem, request.getRequestURI());
        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarParametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String mensagem = "Valor inválido para " + ex.getName() + ": " + ex.getValue();
        ErroResponse erro = new ErroResponse(400, mensagem, request.getRequestURI());
        return ResponseEntity.badRequest().body(erro);
    }
}
