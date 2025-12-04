package com.rms.app.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import com.rms.app.model.RecentProject;
import com.rms.app.service.IGlobalConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class GlobalConfigServiceImpl implements IGlobalConfigService {
    private static final Logger logger = LoggerFactory.getLogger(GlobalConfigServiceImpl.class);
    private static final String APP_DIR = ".baworkbench";
    private static final String CONFIG_FILE = "global.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<RecentProject> recentProjects = new ArrayList<>();
    private final File configFile;

    public GlobalConfigServiceImpl() {
        String userHome = System.getProperty("user.home");
        File appDir = new File(userHome, APP_DIR);
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        this.configFile = new File(appDir, CONFIG_FILE);
        loadConfig();
    }

    private void loadConfig() {
        if (configFile.exists()) {
            try {
                recentProjects = objectMapper.readValue(configFile, new TypeReference<List<RecentProject>>(){});
                // Sắp xếp theo thời gian mở gần nhất
                recentProjects.sort(Comparator.comparingLong(RecentProject::getLastOpened).reversed());
            } catch (IOException e) {
                logger.error("Không thể đọc global config", e);
                recentProjects = new ArrayList<>();
            }
        }
    }

    @Override
    public void saveConfig() {
        try {
            objectMapper.writeValue(configFile, recentProjects);
        } catch (IOException e) {
            logger.error("Không thể lưu global config", e);
        }
    }

    @Override
    public List<RecentProject> getRecentProjects() {
        return recentProjects;
    }

    @Override
    public void addRecentProject(String name, String path) {
        // Xóa nếu đã tồn tại để đưa lên đầu
        recentProjects.removeIf(p -> p.getPath().equals(path));

        recentProjects.add(0, new RecentProject(name, path, System.currentTimeMillis()));

        // Giới hạn lưu 10 dự án gần nhất
        if (recentProjects.size() > 10) {
            recentProjects = recentProjects.subList(0, 10);
        }
        saveConfig();
    }
}