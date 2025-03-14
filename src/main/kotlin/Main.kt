package org.example

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.Scanner

data class GitHubUser(
    val login: String,
    @SerializedName("followers") val followersCount: Int,
    @SerializedName("following") val followingCount: Int,
    @SerializedName("created_at") val createdAt: String
)

data class Repo(
    val name: String
)

interface GitHubApi {
    @GET("users/{username}")
    suspend fun getUser(@Path("username") username: String): GitHubUser

    @GET("users/{username}/repos")
    suspend fun getRepos(@Path("username") username: String): List<Repo>
}

val userCache = mutableMapOf<String, Pair<GitHubUser, List<Repo>>>()

val retrofit = Retrofit.Builder()
    .baseUrl("https://api.github.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()
val api = retrofit.create(GitHubApi::class.java)

suspend fun getUserInfo(username: String) {
    if (userCache.containsKey(username)) {
        println("Data loaded from cache: ")
        printUserInfo(userCache[username]!!.first, userCache[username]!!.second)
        return
    }
    try {
        val user = api.getUser(username)
        val repos = api.getRepos(username)
        userCache[username] = Pair(user, repos)
        printUserInfo(user, repos)
    } catch (e: Exception) {
        println("Error fetching data: ${e.message}")
    }
}

fun printUserInfo(user: GitHubUser, repos: List<Repo>) {
    println("Username: ${user.login}")
    println("Followers: ${user.followersCount}")
    println("Following: ${user.followingCount}")
    println("Account created at: ${user.createdAt}")
    println("Repositories: ${repos.joinToString { it.name }}")
}

fun listStoredUsers() {
    if (userCache.isEmpty()) {
        println("هیچ کاربری در حافظه موجود نیست.")
        return
    }
    userCache.keys.forEach { println(it) }
}

fun searchUser(username: String) {
    val user = userCache[username]
    if (user != null) {
        printUserInfo(user.first, user.second)
    } else {
        println("No users found in cache.")
    }
}

fun searchByRepoName(repoName: String) {
    val results = userCache.filterValues { it.second.any { repo -> repo.name.contains(repoName, ignoreCase = true) } }
    if (results.isEmpty()) {
        println("Repository not found.")
    } else {
        results.forEach { (username, data) ->
            println(
                "User: $username has repository: ${
                    data.second.find {
                        it.name.contains(
                            repoName,
                            ignoreCase = true
                        )
                    }?.name
                }"
            )
        }
    }
}

fun main() {
    val scanner = Scanner(System.`in`)
    while (true) {
        println("\nMenu:")
        println("1️⃣ Fetch user information")
        println("2️⃣ List stored users")
        println("3️⃣ Search user by username")
        println("4️⃣ Search by repository name")
        println("5️⃣ Exit")
        print("Choose an option: ")
        when (scanner.nextInt()) {
            1 -> {
                print("Enter username: ")
                val username = scanner.next()
                kotlinx.coroutines.runBlocking { getUserInfo(username) }
            }

            2 -> listStoredUsers()
            3 -> {
                print("Enter username: ")
                val username = scanner.next()
                searchUser(username)
            }

            4 -> {
                print("Enter username: ")
                val repoName = scanner.next()
                searchByRepoName(repoName)
            }

            5 -> return
            else -> println("Invalid option.")
        }
    }
}
