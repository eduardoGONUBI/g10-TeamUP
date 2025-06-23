package com.example.teamup.ui.screens.main.UserManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.teamup.domain.usecase.LoginUseCase
import com.example.teamup.data.local.SessionRepository

// cria instancias do view model

class LoginViewModelFactory(
    private val loginUseCase: LoginUseCase,
    private val sessionRepo: SessionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LoginViewModel(loginUseCase, sessionRepo) as T
}

class RegisterViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RegisterViewModel() as T
    }
}