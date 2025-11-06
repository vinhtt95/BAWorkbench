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
        // Bind ViewModel để View có thể Inject
        // Theo kiến trúc, ViewModel sẽ là Singleton hoặc Scoped
        // nhưng hiện tại ta bind đơn giản
        bind(MainViewModel.class);

        // Bind Services (DIP)
        bind(IProjectService.class).to(ProjectServiceImpl.class).in(Singleton.class);

        // Bind Repositories (DIP)
        // Lớp Service/ViewModel sẽ @Inject IArtifactRepository
        bind(IArtifactRepository.class).to(JsonFileRepository.class).in(Singleton.class);
    }
}