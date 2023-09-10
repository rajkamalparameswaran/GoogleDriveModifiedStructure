package com.isteer.googledrive.service.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.isteer.googledrive.service.GoogleDriveService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class GoogleDriveServiceImpl implements GoogleDriveService {

	@Value("${google.secret.key.path}")
	private Resource clientSecret;

	@Value("${google.credentials.folder.path}")
	private Resource credentialFolder;

	@Value("${google.oauth.callback.uri}")
	private String redirectUrl;

	@Value("${google.application.name}")
	private String applicationName;

	@Value("${google.dummy.user.identifier}")
	private String userIdentifier;

	@Value("${google.signIn.url}")
	private String signInUrl;

	private GoogleAuthorizationCodeFlow authorizationCodeFlow;

	private HttpTransport httpTransport = new NetHttpTransport();

	private JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

	@Override
	public void init() {
		try {
			GoogleClientSecrets googleClientSecrets = GoogleClientSecrets.load(jsonFactory,
					new InputStreamReader(clientSecret.getInputStream()));
			authorizationCodeFlow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory,
					googleClientSecrets, Arrays.asList(DriveScopes.DRIVE))
					.setDataStoreFactory(new FileDataStoreFactory(credentialFolder.getFile())).build();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void doSignIn(HttpServletResponse httpServletResponse) {
		try {
			GoogleAuthorizationCodeRequestUrl googleAuthorizationCodeRequestUrl = authorizationCodeFlow
					.newAuthorizationUrl();
			String url = googleAuthorizationCodeRequestUrl.setRedirectUri(redirectUrl).setAccessType("offline").build();
			httpServletResponse.sendRedirect(url);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void saveToken(HttpServletResponse httpServletResponse, HttpServletRequest httpServletRequest)
			throws IOException {

		String authorizationCode = httpServletRequest.getParameter("code");
		if (authorizationCode != null) {
			GoogleTokenResponse accessToken = authorizationCodeFlow.newTokenRequest(authorizationCode)
					.setRedirectUri(redirectUrl).execute();
			authorizationCodeFlow.createAndStoreCredential(accessToken, userIdentifier);
			httpServletResponse.getWriter().write("Welcome to the home Page");
		} else {
			httpServletResponse.sendRedirect(signInUrl);
		}

	}

	@Override
	public boolean createFolderInGoogleDrive(String folderName) throws IOException {
		Credential credential = authorizationCodeFlow.loadCredential(userIdentifier);
		Drive drive = new Drive.Builder(httpTransport, jsonFactory, credential).setApplicationName(applicationName)
				.build();
		if (getFolderIdOrFileIdByName(folderName, drive) == null) {
			File file = new File();
			file.setName(folderName);
			file.setMimeType("application/vnd.google-apps.folder");
			drive.files().create(file).execute();
			return true;
		}
		return false;
	}

	private String getFolderIdOrFileIdByName(String folderName, Drive drive) throws IOException {
		String query = "name='" + folderName + "' and trashed=false";
		FileList fileList = drive.files().list().setQ(query).setFields("files(id)").execute();
		if (fileList.getFiles().isEmpty()) {
			return null;
		}
		return fileList.getFiles().get(0).getId();

	}

	private String getSpecificFileInFolder(String fileName, String parentFolderId, Drive drive) throws IOException {
		String query = "name='" + fileName + "' and '" + parentFolderId + "' in parents and trashed=false";
		FileList fileList = drive.files().list().setQ(query).setFields("files(id)").execute();
		if (fileList.getFiles().isEmpty()) {
			return null;
		}
		return fileList.getFiles().get(0).getId();

	}

	@Override
	public boolean uploadFileInGoogleDriveFolder(MultipartFile multiPartfile, String folderName) throws IOException {
		Credential credential = authorizationCodeFlow.loadCredential(userIdentifier);
		Drive drive = new Drive.Builder(httpTransport, jsonFactory, credential).setApplicationName(applicationName)
				.build();
		String folderId = getFolderIdOrFileIdByName(folderName, drive);
		if (folderId != null) {
			return uploadFileInToDrive(multiPartfile, drive, folderId);
		} else {
			createFolderInGoogleDrive(folderName);
			return uploadFileInToDrive(multiPartfile, drive, getFolderIdOrFileIdByName(folderName, drive));

		}

	}

	public boolean uploadFileInToDrive(MultipartFile multiPartfile, Drive drive, String folderId) throws IOException {
		if (getSpecificFileInFolder(multiPartfile.getOriginalFilename(), folderId, drive) == null) {
			File file = new File();
			file.setName(multiPartfile.getOriginalFilename());
			file.setParents(Arrays.asList(folderId));
			String tempDirectory = System.getProperty("java.io.tmpdir");
			String tempFilePath = tempDirectory + java.io.File.separator + file.getName();
			java.io.File tempFile = new java.io.File(tempFilePath);
			multiPartfile.transferTo(tempFile);
			FileContent fileContent = new FileContent(multiPartfile.getContentType(), tempFile);
			drive.files().create(file, fileContent).setFields("id").execute();
			tempFile.delete();
			return true;
		}
		return false;

	}

	@Override
	public boolean deleteFileFromGoogleDriveFolder(String folderName, String fileName) throws IOException {
		Credential credential = authorizationCodeFlow.loadCredential(userIdentifier);
		Drive drive = new Drive.Builder(httpTransport, jsonFactory, credential).setApplicationName(applicationName)
				.build();
		String folderId = getFolderIdOrFileIdByName(folderName, drive);
		String fileId = getSpecificFileInFolder(fileName, folderId, drive);
		if (folderId != null && fileId != null) {
			drive.files().delete(fileId).execute();
			return true;
		}

		return false;
	}

	@Override
	public boolean getFileFromGoogleDrive(String folderName, String fileName, HttpServletResponse httpServletResponse)
			throws IOException {
		Credential credential = authorizationCodeFlow.loadCredential(userIdentifier);
		Drive drive = new Drive.Builder(httpTransport, jsonFactory, credential).setApplicationName(applicationName)
				.build();
		String folderId = getFolderIdOrFileIdByName(folderName, drive);
		String fileId = getSpecificFileInFolder(fileName, folderId, drive);
		if (folderId != null && fileId != null) {
			File file = drive.files().get(fileId).execute();
			httpServletResponse.setContentType(file.getMimeType());
			// httpServletResponse.setHeader("Content-Disposition", "attachment;
			// filename=\"" + file.getName() + "\"");
			drive.files().get(fileId).executeMediaAndDownloadTo(httpServletResponse.getOutputStream());
			return true;
		}
		return false;
	}

	@Override
	public List<String> getAllFilesLink(String folderName) throws IOException {
		Credential credential = authorizationCodeFlow.loadCredential(userIdentifier);
		Drive drive = new Drive.Builder(httpTransport, jsonFactory, credential).setApplicationName(applicationName)
				.build();
		String folderId = getFolderIdOrFileIdByName(folderName, drive);
		String query = "'" + folderId + "' in parents and trashed=false";
		FileList fileList = drive.files().list().setQ(query).setFields("files(webViewLink)").execute();
		return fileList.getFiles().stream().map(File::getWebViewLink).toList();

	}
}
