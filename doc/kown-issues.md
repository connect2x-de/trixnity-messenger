# Known Issues

## Audio recorder: Removing permission while recording results in corrupt file on Chrome

Reproduce: use Chrome -> give permissions -> start recording -> revoke permissions/unplug mic -> stop and send message

Will result in a (corrupted?) audio file which only plays until the moment the mic was recording and a trailing unplayable part. Important: All audio which was recorded before the permission was revoked can still be played normally.

This bug seems obscure enough, and therefore it should be okay to leave it unfixed.

# Motivation for this file

A usual issue tracker is often full of small bugs and other issues with very low priority. Often we cannot see any time in the plannable future where these issues are fixed. This can massively clutter the issue tracker and makes planning into the near or midterm future hard, especially for the product owner.

We want to move these kind of issues away from our main issue tracker but do not want to lose them. Especially because it can turn out that some issues are in fact important enough to be moved into the main issue tracker.
