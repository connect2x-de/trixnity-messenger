# Shared fundamentals

This information applies to both credentials autofill and credentials saving.

As we are rendering to a canvas, we need to trick the browser into believing that it should autofill and save credentials.
For that we more or less only have a very simple requirement: A single HTMLFormElement with two HTMLInputElements inside.

HOWEVER, each browser and password management engine, whether builtin or internal, has different quirks that are listed following.

# Credential Autofill

- Require `type="text", autocomplete="username"` and `type="password", autocomplete="current-password"` on the two HTMLInputElements. (All browsers)
- Requires the password HTMLInputElement to be hit-testable^[1] (Bitwarden, KeepassXC, Edge)
- Requires real user interaction with at least one HTMLInputElement (Chromium based, Firefox)
- Requires real user interaction with the username HTMLInputElement (Firefox)

Basics to satisfy all criteria is (for details please take a look at the code):

1. Create a form with two HTMLInputElements and the above-mentioned attributes
2. Create a style to ensure nothing is visible while the HTMLInputElements are still hit-testable
3. Add EventListeners to disallow pointer and keyboard interaction
4. Place the username HTMLInputElement exactly over a Jetpack Compose button
5. Add EventListeners to delegate pointer events to the Jetpack Compose button

The user can thus interact with the Autofill button in compose while actually initiating a real user interaction with the HTMLInputElement.

# Credential Saving

- Require `type="text", autocomplete="username"` and `type="password", autocomplete="new-password"` on the two HTMLInputElements. (All browsers)
- Requires a change to the history (All browsers)
- Requires the HTMLFormElement to disappear (All Browsers)
- Requires the password HTMLInputElement to be hit-testable^[1] (Bitwarden, KeepassXC, Edge)
- Requires FocusEvents, InputEvents and ChangeEvents (Bitwarden, KeepassXC, Edge)

Basics to satisfy all criteria is (for details please take a look at the code):

1. Create a form with two HTMLInputElements and the above-mentioned attributes
2. Create a style to ensure nothing is visible while the HTMLInputElements are still hit-testable
3. Add EventListeners to disallow pointer and keyboard interaction
4. Simulate typing into the HTMLInputElements
5. Replace the history with itself
6. Detach and reattach the HTMLFormElement

As this, in contrary to the autofill, does not require any real user interaction, step 4-6 can be a simple function initiating the credential saving.

[^1]: hit-testable means that there has to be a large enough surface where a user could potentially interact with the element, more specifically:
        - The HTMLInputElement has to be laid out (no `display :none;`, `opacity: 0;` or `pointer-events: none;`)
        - The HTMLInputElement has to be positioned inside the viewport and cannot overlap
        - The HTMLInputElement needs to be the topmost element (e.g. either hierarchically via the DOM or via `z-index`)
        - The HTMLInputElement needs a minimum size, typically more than 10 by 10 pixels

