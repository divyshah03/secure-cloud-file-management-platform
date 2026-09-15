package com.cloudfilemanager.file;

import com.cloudfilemanager.audit.AuditLogService;
import com.cloudfilemanager.file.sharing.FilePermissionService;
import com.cloudfilemanager.malware.MalwareScanner;
import com.cloudfilemanager.malware.ScanResult;
import com.cloudfilemanager.storage.S3Buckets;
import com.cloudfilemanager.storage.S3Service;
import com.cloudfilemanager.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileDtoMapper fileDtoMapper;

    @Mock
    private S3Service s3Service;

    @Mock
    private S3Buckets s3Buckets;

    @Mock
    private MalwareScanner malwareScanner;

    @Mock
    private FilePermissionService filePermissionService;

    @Mock
    private AuditLogService auditLogService;

    private FileService fileService;

    private User owner;

    @BeforeEach
    void setUp() {
        // Constructed explicitly rather than via @InjectMocks: Mockito's constructor-based
        // auto-injection doesn't reliably resolve a trailing @Value-annotated primitive
        // parameter (mockStorage) alongside mocked reference-type parameters.
        fileService = new FileService(
                fileRepository, fileDtoMapper, s3Service, s3Buckets,
                filePermissionService, malwareScanner, auditLogService, false);

        owner = new User("Test User", "test@example.com", "password");
        owner.setId(1L);
    }

    @Test
    void uploadFileRejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> fileService.uploadFile(empty, owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");

        verify(s3Service, never()).putObject(anyString(), anyString(), any());
        verify(fileRepository, never()).save(any());
    }

    @Test
    void uploadFileRejectsOversizedFile() {
        byte[] oversized = new byte[(int) (50 * 1024 * 1024) + 1];
        MockMultipartFile file = new MockMultipartFile("file", "big.bin", "application/octet-stream", oversized);

        assertThatThrownBy(() -> fileService.uploadFile(file, owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50 MB");

        verify(s3Service, never()).putObject(anyString(), anyString(), any());
    }

    @Test
    void deleteFileRemovesObjectFromS3AndRepository() {
        File file = new File(
                "stored.bin",
                "original.bin",
                100L,
                "application/octet-stream",
                "files/1/stored.bin",
                "filemanager-files",
                owner
        );
        file.setId(42L);

        when(fileRepository.findByIdAndOwner(42L, owner)).thenReturn(Optional.of(file));

        fileService.deleteFile(42L, owner);

        verify(s3Service).deleteObject("filemanager-files", "files/1/stored.bin");
        verify(fileRepository).delete(file);
    }

    @Test
    void uploadFileStoresObjectAndPersistsMetadata() throws Exception {
        when(s3Buckets.getFiles()).thenReturn("filemanager-files");
        when(malwareScanner.scan(any())).thenReturn(ScanResult.clean());
        when(fileRepository.save(any(File.class))).thenAnswer(invocation -> {
            File saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "hello".getBytes()
        );

        var response = fileService.uploadFile(multipartFile, owner);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3Service).putObject(eq("filemanager-files"), keyCaptor.capture(), any(byte[].class));
        assertThat(keyCaptor.getValue()).startsWith("files/1/");
        assertThat(response.fileId()).isEqualTo(7L);
        assertThat(response.originalFileName()).isEqualTo("notes.txt");
    }

    @Test
    void uploadFileRejectsInfectedFile() {
        when(malwareScanner.scan(any())).thenReturn(ScanResult.infected("Eicar-Test-Signature"));

        MockMultipartFile multipartFile = new MockMultipartFile(
                "file", "eicar.txt", "text/plain", "fake-eicar-bytes".getBytes());

        assertThatThrownBy(() -> fileService.uploadFile(multipartFile, owner))
                .isInstanceOf(com.cloudfilemanager.malware.MalwareDetectedException.class)
                .hasMessageContaining("Eicar-Test-Signature");

        verify(s3Service, never()).putObject(anyString(), anyString(), any());
        verify(fileRepository, never()).save(any());
    }
}
