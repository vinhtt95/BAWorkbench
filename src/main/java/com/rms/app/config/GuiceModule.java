package com.rms.app.config;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.rms.app.repository.JsonFileRepository;
import com.rms.app.service.IArtifactRepository;
import com.rms.app.service.IProjectService;
import com.rms.app.service.impl.ProjectServiceImpl;
import com.rms.app.viewmodel.MainViewModel;

public class GuiceModule extends AbstractModule {
    @Override
    protected void configure() {
        // Đây là điều BẮT BUỘC để View và Repository
        // chia sẻ cùng một state (ví dụ: thư mục dự án hiện tại).
        bind(MainViewModel.class).in(Singleton.class);

        // Bind Services (DIP)
        bind(IProjectService.class).to(ProjectServiceImpl.class).in(Singleton.class);

        // Bind Repositories (DIP)
        bind(IArtifactRepository.class).to(JsonFileRepository.class).in(Singleton.class);
    }
}