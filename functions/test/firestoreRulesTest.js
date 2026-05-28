const fs = require('fs');
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');

(async () => {
  const projectId = 'demo-project';
  const rules = fs.readFileSync('../firestore.rules', 'utf8');

  const env = await initializeTestEnvironment({
    projectId,
    firestore: { rules }
  });

  try {
    // Alice creates a story (should succeed)
    const alice = env.authenticatedContext('alice');
    const aliceDb = alice.firestore();
    const now = Date.now();
    const expire = now + 86400000;
    const storyRef = aliceDb.doc('stories/s1');
    await assertSucceeds(storyRef.set({ ownerId: 'alice', note: 'Hello', privacy: 'friends', createdAt: now, expiresAt: expire }));
    console.log('Alice created story: OK');

    // Signed-in users can load the story feed query
    const storyQuery = aliceDb.collection('stories').orderBy('createdAt', 'desc').limit(64);
    await assertSucceeds(storyQuery.get());
    console.log('Alice can list stories: OK');

    // Alice can save her draft
    const draftRef = aliceDb.doc('storyDrafts/alice');
    await assertSucceeds(draftRef.set({ note: 'Draft', updatedAt: now }));
    console.log('Alice can save draft: OK');

    // Bob tries to read the story (should fail before following)
    const bob = env.authenticatedContext('bob');
    const bobDb = bob.firestore();
    const bobStoryRef = bobDb.doc('stories/s1');
    await assertFails(bobStoryRef.get());
    console.log('Bob cannot read story before following: OK');

    // Bob follows Alice
    const bobFollowerRef = bobDb.doc('users/alice/followers/bob');
    await assertSucceeds(bobFollowerRef.set({ userId: 'bob', createdAt: now }));
    console.log('Bob followed Alice: OK');

    // Now Bob can read the story
    await assertSucceeds(bobStoryRef.get());
    console.log('Bob can read story after following: OK');

    // Test view write by Bob (views subcollection)
    const viewRef = bobDb.doc('stories/s1/views/bob');
    await assertSucceeds(viewRef.set({ viewerId: 'bob', seenAt: now }));
    console.log('Bob can write his view record: OK');

    // Story reply creation by Bob to Alice's story (should succeed)
    const replyRef = bobDb.doc('storyReplies/r1');
    await assertSucceeds(replyRef.set({ senderId: 'bob', receiverId: 'alice', storyOwnerId: 'alice', storyId: 's1', createdAt: now, message: 'Nice!' }));
    console.log('Bob created a reply: OK');

    // Alice can read the reply
    const aliceReplyRef = aliceDb.doc('storyReplies/r1');
    await assertSucceeds(aliceReplyRef.get());
    console.log('Alice can read reply: OK');

  } catch (err) {
    console.error('Test failed', err);
  } finally {
    await env.cleanup();
  }
})();
