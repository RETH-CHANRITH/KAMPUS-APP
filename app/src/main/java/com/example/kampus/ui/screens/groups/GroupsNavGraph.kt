package com.example.kampus.ui.screens.groups

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.example.kampus.viewmodel.GroupsViewModel

object GroupRoutes {
    const val GRAPH = "groups_graph"
    const val LIST = "groups/list"
    const val DETAIL = "groups/detail/{groupId}"
    const val CREATE = "groups/create"
    const val ADMIN = "groups/admin/{groupId}"

    fun detail(id: String) = "groups/detail/$id"
    fun admin(id: String) = "groups/admin/$id"
}

fun NavGraphBuilder.groupsNavGraph(
    navController: NavController,
    sharedViewModel: GroupsViewModel,
) {
    navigation(startDestination = GroupRoutes.LIST, route = GroupRoutes.GRAPH) {
        composable(GroupRoutes.LIST) {
            GroupsScreen(
                viewModel = sharedViewModel,
                onCreateGroupClick = { navController.navigate(GroupRoutes.CREATE) },
                onGroupClick = { groupId -> navController.navigate(GroupRoutes.detail(groupId)) },
                onHomeClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onEventsClick = { navController.navigate("event_list") },
                onChatClick = { navController.navigate("chat_list") },
                onProfileClick = { navController.navigate("profile") },
                onCreatePost = { navController.navigate("post_create") },
            )
        }

        composable(GroupRoutes.CREATE) {
            CreateGroupScreen(
                viewModel = sharedViewModel,
                onBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack() },
            )
        }

        composable(
            route = GroupRoutes.DETAIL,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            GroupDetailScreen(
                groupId = groupId,
                viewModel = sharedViewModel,
                onBack = { navController.popBackStack() },
                onOpenAdminPanel = { navController.navigate(GroupRoutes.admin(groupId)) },
                onHomeClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onEventsClick = { navController.navigate("event_list") },
                onChatClick = { navController.navigate("chat_list") },
                onProfileClick = { navController.navigate("profile") },
                onCreatePost = { navController.navigate("post_create") },
            )
        }

        composable(
            route = GroupRoutes.ADMIN,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            AdminPanelScreen(
                groupId = groupId,
                viewModel = sharedViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
    // Example:
    // NavHost(navController = navController, startDestination = "home") {
    //     groupsNavGraph(navController = navController, sharedViewModel = groupsViewModel)
    // }
}