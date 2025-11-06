package com.rms.app.config;

import com.google.inject.AbstractModule;
import com.rms.app.viewmodel.MainViewModel;

public class GuiceModule extends AbstractModule {
    @Override
    protected void configure() {
        // Bind ViewModel để View có thể Inject
        // Theo kiến trúc, ViewModel sẽ là Singleton hoặc Scoped
        // nhưng hiện tại ta bind đơn giản
        bind(MainViewModel.class);
    }
}