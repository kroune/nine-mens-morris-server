package appVersions.di

import appVersions.data.dao.VersionDataServiceI
import appVersions.data.dao.VersionDataServiceImpl
import database.DatabaseConfiguration
import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.dsl.module

val versionModule = module {
    single<VersionDataServiceI> { VersionDataServiceImpl(get<Database>(named(DatabaseConfiguration.VERSION_DATA))) }
}