package com.isteer.google.drive.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.isteer.Response;
import com.isteer.googledrive.service.GoogleDriveService;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/GoogleDrive")
public class GoogleDriveController {
	
	@Autowired
	GoogleDriveService googleDriveService;

	@PostConstruct
	public void init() {
		googleDriveService.init();
	}

	@GetMapping("/SignIn")
	public void googleSignIn(HttpServletResponse httpServletResponse) {
		googleDriveService.doSignIn(httpServletResponse);
	}

	@GetMapping("/Home")
	public ResponseEntity<Object> homePage(HttpServletResponse httpServletResponse,
			HttpServletRequest httpServletRequest) {
		try {
			googleDriveService.saveToken(httpServletResponse, httpServletRequest);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (TokenResponseException e) {
			return new ResponseEntity<>(e.getDetails(), HttpStatus.BAD_REQUEST);
		} catch (IOException e) {
			return new ResponseEntity<>(e.getLocalizedMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/CreateFolder/{folderName}")
	public ResponseEntity<Object> createFolderInGoogleDrive(@PathVariable String folderName) {
		try {
			boolean response = googleDriveService.createFolderInGoogleDrive(folderName);
			if (response) {
				return new ResponseEntity<>(new Response(1, folderName + " this folder created sucessfully"),
						HttpStatus.CREATED);
			} else {
				return new ResponseEntity<>(new Response(-1, folderName + " this folder Name Already Exists"),
						HttpStatus.BAD_REQUEST);
			}
		} catch (GoogleJsonResponseException | TokenResponseException e) {
			return new ResponseEntity<>(new Response(-1, e.getStatusMessage()), HttpStatus.valueOf(e.getStatusCode()));
		} catch (IOException e) {
			return new ResponseEntity<>(new Response(-1, e.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/UplodaFileInGoogleDrive/{folderName}")
	public ResponseEntity<Object> uploadFileInGoogleDrive(@RequestParam("file") MultipartFile file,
			@PathVariable String folderName) {
		try {
			boolean created = googleDriveService.uploadFileInGoogleDriveFolder(file, folderName);
			if (created) {
				return ResponseEntity.status(HttpStatus.CREATED)
						.body(new Response(1, file.getOriginalFilename() + " File Uploded SucessFully"));
			}
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new Response(-1, " File Name Or Folder Name Already Exists"));
		} catch (GoogleJsonResponseException | TokenResponseException e) {
			return new ResponseEntity<>(new Response(-1, e.getStatusMessage()), HttpStatus.valueOf(e.getStatusCode()));
		} catch (IOException e) {
			return new ResponseEntity<>(new Response(-1, e.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	@DeleteMapping("/DeleteFileFromGooleDriveFolder/{folderName}/{fileName}")
	public ResponseEntity<Object> getFileFromGoogleDriveFolder(@PathVariable String folderName,
			@PathVariable String fileName) {
		try {
			boolean fileDeleted = googleDriveService.deleteFileFromGoogleDriveFolder(folderName, fileName);
			if (fileDeleted) {
				return ResponseEntity.status(HttpStatus.OK)
						.body(new Response(1, fileName + " File Deleted SucessFully"));
			}
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(new Response(-1, " File Name Or Folder Name Not Found"));
		} catch (GoogleJsonResponseException | TokenResponseException e) {
			return new ResponseEntity<>(new Response(-1, e.getStatusMessage()), HttpStatus.valueOf(e.getStatusCode()));
		} catch (IOException e) {
			return new ResponseEntity<>(new Response(-1, e.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/GetFileFromGoogleDrive/{folderName}/{fileName}")
	public ResponseEntity<Object> retrieveFileFromGoogleDrive(@PathVariable String folderName,
			@PathVariable String fileName, HttpServletResponse httpServletResponse) {
		try {
			boolean response = googleDriveService.getFileFromGoogleDrive(folderName, fileName, httpServletResponse);
			if (response) {
				return new ResponseEntity<>(HttpStatus.FOUND);
			} else {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(new Response(-1, " File Name Or Folder Name Not Found"));
			}
		} catch (GoogleJsonResponseException | TokenResponseException e) {
			return new ResponseEntity<>(new Response(-1, e.getStatusMessage()), HttpStatus.valueOf(e.getStatusCode()));
		} catch (IOException e) {
			return new ResponseEntity<>(new Response(-1, e.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
		}
	}

	@GetMapping("/GetAllFilesLinkFromFolder/{folderName}")
	public ResponseEntity<Object> retrieveAllFilesFromGoogleDrive(@PathVariable String folderName) {
		try {
			return ResponseEntity.status(HttpStatus.OK).body(googleDriveService.getAllFilesLink(folderName));
		} catch (GoogleJsonResponseException | TokenResponseException e) {
			return new ResponseEntity<>(new Response(-1, e.getStatusMessage()), HttpStatus.valueOf(e.getStatusCode()));
		} catch (IOException e) {
			return new ResponseEntity<>(new Response(-1, e.getLocalizedMessage()), HttpStatus.BAD_REQUEST);
		}
	}

}
