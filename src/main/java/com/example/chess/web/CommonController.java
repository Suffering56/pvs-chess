package com.example.chess.web;

import com.example.chess.App;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CommonController {

	@GetMapping("/version")
	public ResponseEntity<String> getVersion() {
		return ResponseEntity.ok(App.getVersion());
	}
}
