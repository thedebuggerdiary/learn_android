package com.debuggerdiary.ep07

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRepo(
    val id: Long,
    val name: String,
    val description: String?,

    @SerialName("stargazers_count")
    val stars: Int,

    @SerialName("html_url")
    val htmlUrl: String,

    @SerialName("full_name")
    val fullName: String,

    val owner: Owner
)

@Serializable
data class Owner(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String
)
