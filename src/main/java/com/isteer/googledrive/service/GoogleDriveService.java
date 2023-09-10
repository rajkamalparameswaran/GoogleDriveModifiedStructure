package com.isteer.googledrive.service;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface GoogleDriveService {

	public void init();

	public void doSignIn(HttpServletResponse httpServletResponse);

	void saveToken(HttpServletResponse httpServletResponse, HttpServletRequest httpServletRequest) throws IOException;

	public boolean createFolderInGoogleDrive(String folderName) throws IOException;

	public boolean deleteFileFromGoogleDriveFolder(String folderName, String fileName) throws IOException;

	public boolean uploadFileInGoogleDriveFolder(MultipartFile multiPartfile, String folderName) throws IOException;

	public boolean getFileFromGoogleDrive(String folderName, String fileName,HttpServletResponse httpServletResponse) throws IOException;

	public List<String> getAllFilesLink(String folderName) throws IOException;

}
