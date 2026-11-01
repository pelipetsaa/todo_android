package com.pelypets.todolist.data.di

import javax.inject.Qualifier

/** Outlives every screen: for work that must finish even if the user leaves, e.g. revoking a token. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
