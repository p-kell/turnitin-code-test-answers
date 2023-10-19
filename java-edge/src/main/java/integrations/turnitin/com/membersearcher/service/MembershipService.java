package integrations.turnitin.com.membersearcher.service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import integrations.turnitin.com.membersearcher.client.MembershipBackendClient;
import integrations.turnitin.com.membersearcher.model.MembershipList;

import integrations.turnitin.com.membersearcher.model.User;
import integrations.turnitin.com.membersearcher.model.UserList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MembershipService {
	@Autowired
	private MembershipBackendClient membershipBackendClient;


	/**
	 * findOneById method essentially replaces the fetchUser(member.getId) from previous method
  	 * It will fetch one user by userID and returns that user.
	 * The new fetchAllMembershipsWithUsers method will fetch all memberships once and fetch all users once,
  	 * and will then iterate over memberships, use already fetched list of users to get linked by userId
	 * @return A CompletableFuture of MembershipList with incluced users.
	 */
	
	public CompletableFuture<User> findOneById(CompletableFuture<UserList> allUsers, String userId ) {
		return allUsers.thenCompose(users -> users.getUsers().stream().filter(u -> Objects.equals(u.getId(), userId))
				.map(CompletableFuture::completedFuture)
				.toList().get(0));
	}

	public CompletableFuture<MembershipList> fetchAllMembershipsWithUsers() {
		CompletableFuture<UserList> allUsers = membershipBackendClient.fetchUsers();
		return membershipBackendClient.fetchMemberships()
				.thenCompose(members -> {
					CompletableFuture<?>[] userCalls = members.getMemberships().stream()
							.map(member -> findOneById(allUsers, member.getUserId())
									.thenApply(member::setUser))
							.toArray(CompletableFuture<?>[]::new);
					return CompletableFuture.allOf(userCalls)
							.thenApply(nil -> members);
				});
	}
}

