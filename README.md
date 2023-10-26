# Trixnity Messenger - A headless Matrix messenger SDK

Trixnity Messenger provides extensions on top of [Trixnity](https://gitlab.com/trixnity/trixnity)
geared towards building a multiplatform messenger. It is agnostic to the UI and supports all technologies that are
interoperable with
Kotlin. UI frameworks that are reactive like
[Compose Multiplatform](https://www.jetbrains.com/lp/compose-mpp), [SwiftUI](https://developer.apple.com/xcode/swiftui/)
, or [ReactJS](https://reactjs.org/) are best suited, since the changes in Trixnity Messenger can be reflected in the
component by binding to the view model.

**You need help? Ask your questions
in [#trixnity-messenger:imbitbu.de](https://matrix.to/#/#trixnity-messenger:imbitbu.de).**

## TI-Messenger

Are you looking for a TI-Messenger SDK? Trixnity Messenger is the base for our TIM SDK. If you want to know more,
contact us at [contact@connect2x.de](mailto:contact@connect2x.de).

## MVVM

Trixnity Messenger follows the MVVM pattern, in particular it represents the view model (VM) part. Trixnity is the
model (M), containing all Matrix-related logic. The view (V) on top of Trixnity Messenger is the presentation geared
towards the user.

![](doc/MVVM.png)

This patterns frees the UI layer from lots of logic that can be complicated (and thus needs to be tested). In an ideal
case the UI just consumes information provided by the view model and presents it. When user interaction occurs, the
corresponding methods in the view model are called (which can lead to changes in the model and therefore the view model)
.

This is an overview on how different UI technologies can be used on top of trixnity-messenger:

![](doc/trixnity-messenger-arch.png)

## Getting Started

First you need to add the maven repository:

```kotlin
maven("https://gitlab.com/api/v4/projects/47538655/packages/maven")
```

Now you are able to add trixnity-messenger as dependency to your project:

```kotlin
implementation("de.connect2x:trixnity-messenger:<version>")
```

Just create the root node of the view model tree that is used in your app.

```kotlin
val rootViewModel = RootViewModelImpl()
```

Create a root node in your UI framework and pass the created `rootViewModel` to it. In Compose Multiplatform on the
desktop, it looks something like this:

```kotlin
application {
    Window("My App") {
        MyMatrixClient(rootViewModel)
    }
}
```

where `MyMatrixClient` is a `@Composable` function that gets the `RootViewModel` as a parameter.

Now you are ready to react to different states of the routing in the `RootViewModel`.

### Override defaults

There are some parameters of `RootViewModelImpl`, that you can override to change the behaviour.

* The `KoinApplication` (see [DI](#change-the-default-behavior-of-view-models)) can be used to override view models and
  other components.
  ```kotlin
  val koinApplication = koinApplication {
    modules(trixnityMessengerModule())
  }
  ```
* The `MatrixClientService` (holds `MatrixClient`s that can be used to access Matrix APIs,
  see [Trixnity](https://gitlab.com/trixnity/trixnity)) can be extended to a service (e. g. for Android).
  ```kotlin
     val matrixClientService = DefaultMatrixClientService(koinApplication.koin)
  ```
* The `ComponentContext` (from [Decompose](https://github.com/arkivanov/Decompose)) may need to be adapted to your UI
  technology.
  ```kotlin
  val componentContext = DefaultComponentContext(LifecycleRegistry())
  ```

### Routing

The `RootViewModel` itself does not do much on its own, but is a point where routing kicks in. Different views in the
view models are organized in stacks that show one view on top and possibly some views behind the top stack (
see [Decompose routing](https://arkivanov.github.io/Decompose/navigation/overview/)).

In our case, let's have a look at `rootViewModel.rootStack`. It returns
a `Value<ChildStack<RootRouter.Config, RootRouter.RootWrapper>>`, i.e. a value changing over time that is providing the
UI with an instance of `RootRouter.RootWrapper`. In a first step, let's observe this value and react to changes:

```kotlin
// this code has to be called from a `suspend` function
rootViewModel.rootStack.toFlow()
    .mapLatest { it.active.instance }
    .collect { wrapper ->
        when (wrapper) {
            is RootRouter.RootWrapper.None -> {} // draw an empty UI
            is RootRouter.RootWrapper.MatrixClientInitialization -> {} // show initialization of the MatrixClient (aka loading screen)
            else -> {} // add more cases    
        }
    }
```

In case you are using Compose as your UI framework, Decompose has
some [helpers](https://arkivanov.github.io/Decompose/extensions/compose/#navigating-between-composable-components) for
routing.

#### Routing overview

To better understand how the routers are connected, the following (incomplete) overview might help. Many details are
left out for clarity.

![](doc/Routing-RootWrapper.png)

![](doc/Routing-Main.png)

## Configuration

Trixnity Messenger has multiple ways to configure the client to your needs.

### MessengerConfig

The class `MessengerConfig` contains static information that is used to determine some folder names and other data in
the lifecycle of the messenger. To override the standard configuration, put this in your code:

```kotlin
val messengerConfig: MessengerConfig.() -> Unit = {
    appName = "MyMatrixClient"
    // more things you would like to change
}

// before a view model is created
MessengerConfig.instance.apply(messengerConfig)
```

### Change the default behavior of view models

You can customize the messenger SDK to fit your needs with the help of dependency injection (DI). Trixnity Messenger
uses [Koin](https://insert-koin.io/) for this.

Suppose you want to deliver a demo version of your messenger and with it, want to fix the server url when the client
tries to login the user to a Matrix server. To do this, you have to do the following:

* provide an alternative implementation to a view model interface, here `AddMatrixAccountViewModel`

```kotlin
  class MyAddMatrixAccountViewModel(
    viewModelContext: ViewModelContext,
    addMatrixAccountViewModel: AddMatrixAccountViewModelImpl,
) : ViewModelContext by viewModelContext, AddMatrixAccountViewModel by addMatrixAccountViewModel {

    private val isDemoVersion: Boolean = ... // this is computed from the config or a runtime parameter
    val canChangeServerUrl: Boolean = !isDemoVersion
    override val serverUrl: MutableStateFlow<String> =
        MutableStateFlow(if (isDemoVersion) "https://myUrl" else addMatrixAccountViewModel.serverUrl.value)
}
```

Then, we have to register the new view model in a module:

```kotlin
val addMatrixAccountModule = module {
    single<AddMatrixAccountViewModelFactory> {
        object : AddMatrixAccountViewModelFactory {
            override fun newAddMatrixAccountViewModel(
                viewModelContext: ViewModelContext,
                onAddMatrixAccountMethod: (AddMatrixAccountMethod) -> Unit,
                onCancel: () -> Unit
            ): AddMatrixAccountViewModel {
                return MyAddMatrixAccountViewModel(
                    viewModelContext,
                    AddMatrixAccountViewModelImpl(viewModelContext, onAddMatrixAccountMethod, onCancel),
                )
            }
        }
    }
}
```

Finally, add it to the modules of `KoinApplication`:

```kotlin
val koinApplication = koinApplication {
    modules(
        trixnityMessengerModule(),
        addMatrixAccountModule,
    )
}
```

When you start your application with this configuration, the implementation of `AddMatrixAccountViewModel` will be your
customized version `MyAddMatrixAccountViewModel` and your UI can use all the properties and methods of it (maybe a
downcast from the `AddMatrixViewModel` interface is needed).

### i18n

Trixnity Messenger comes with a set of standard translations for some states that can occur. It currently supports
English (en) and German (de). It uses a simple
[Kotlin file](./trixnity-messenger/src/commonMain/kotlin/de/connect2x/trixnity/messenger/i18n/I18n.kt) for all
translations.

It allows the same customizations as view models. In order to change messages, simply override the messages you want to
change by
subclassing [I18nBase](./trixnity-messenger/src/commonMain/kotlin/de/connect2x/trixnity/messenger/i18n/I18nBase.kt).

If you want to add new messages, use the delegation pattern as described
in [View model customization](#change-the-default-behavior-of-view-models) and add more messages.

## Usage from Swift (iOS or Mac)

Trixnity Messenger can also be consumed in Swift code to build native iOS or Mac applications.

### Installation

At this moment, the pipeline for Swift builds has not been automated in the CI (this is on our todo-list). Download the
current
version [here](https://gitlab.com/api/v4/projects/47538655/packages/maven/de/connect2x/trixnity-messenger-kmmbridge/1.0.8-LOCAL/trixnity-messenger-kmmbridge-1.0.8-LOCAL.zip).
You can import the XCFramework locally into your project.

### Initialization

In order to use the library from Swift, always ```import trixnity_messenger``` in your files.

To create an instance of Trixnity Messenger do the following:

```swift
let rootViewModel = RootViewModelImpl.companion.create(koinApplication: IosDIKt.trixnityMessengerApplication())
```

Pass this view model to your root UI node, e.g., a view in SwiftUI:

```swift
var body: some Scene {
    WindowGroup {
        RootView(rootViewModel)
    }
}
```

### Values and Flows

Trixnity Messengers provides many properties that can change over time. Two data types are used: `Value`s and `Flow`s (
with its specializations `StateFlow` and `MutableStateFlow`). To access those values and get informed when they update,
different helpers can be used.

#### Value

Values represent the changes in the routers (the data type is coming
from [decompose](https://arkivanov.github.io/Decompose)).

Use this code to get a Swift wrapper for `Value`s.

```swift
import Foundation
import trixnity_messenger

public class ObservableValue<T : AnyObject> : ObservableObject {
    private let observableValue: Value<T>

    @Published
    var value: T

    private var observer: ((T) -> Void)?
    
    init(_ value: Value<T>) {
        observableValue = value
        self.value = observableValue.value
        observer = { [weak self] value in self?.value = value }
        observableValue.subscribe(observer: observer!)
    }

    deinit {
        observableValue.unsubscribe(observer: self.observer!)
    }
}
```

```swift
import SwiftUI
import trixnity_messenger

@propertyWrapper struct StateValue<T : AnyObject>: DynamicProperty {
    @ObservedObject
    private var obj: ObservableValue<T>

    var wrappedValue: T { obj.value }

    init(_ value: Value<T>) {
        obj = ObservableValue(value)
    }
}
```

This allows for the following code to work in SwiftUI:

```swift
struct RootView: View {
    @StateValue
    private var stack: ChildStack<RootRouter.Config, RootRouter.RootWrapper>
    private var activeStack: RootRouter.RootWrapper { stack.active.instance }
    
    init(_ viewModel: RootViewModel) {
        _stack = StateValue(viewModel.rootStack)
    }
    
    // ...
}
```

Now, you can access `activeStack` in your view and get the current router value all the time.

#### Flows

Trixnity Messenger uses [SKIE](https://skie.touchlab.co/) to generate some helper code to get nicer interfaces of Flows
when accessing them from Swift code. To make it even easier, you can use the following helper:

```swift
func observe<T>(_ stateFlow: SkieSwiftStateFlow<T>, _ assign: (T) -> ()) async {
  for await value in stateFlow.map({$0}) {
      assign(value)
  }
}
```

For some primitive values (`Bool`, `Int`, etc.) you might want to add specializations of this method.

To use flows from SwiftUI, create a wrapper of the view model you want to observe. As an example:

```swift
class AddMatrixAccountViewModelSwift: ObservableObject {
    let delegate: AddMatrixAccountViewModel
    @Published private(set) var serverDiscoveryState: AddMatrixAccountViewModelServerDiscoveryState
    
    init(delegate: AddMatrixAccountViewModel) {
        self.delegate = delegate
        self.serverDiscoveryState = delegate.serverDiscoveryState.value
    }
    
    @MainActor
    func activate() async {
        await observe(delegate.serverDiscoveryState) { self.serverDiscoveryState = $0 }
    }
}
```

It can be initiated like this:

```swift
struct AddMatrixAccountView: View {
 
    @ObservedObject private var viewModel: AddMatrixAccountViewModelSwift
    
    init(_ addMatrixAccountViewModel: AddMatrixAccountViewModel) {
        viewModel = AddMatrixAccountViewModelSwift(delegate: addMatrixAccountViewModel)
    }
    
    // here you can access `viewModel.serverDiscoveryState` and always get the last value
}
.task { // this is important: activate the wrapper view model observation of the original view model
    await viewModel.activate()
}
```

## Contributions

If you want to contribute to the project, you need to sign the [Contributor License Agreement](CLA.md).
See [CLA_instructions.md](CLA_instructions.md) for more instructions.

## Commercial license and support

If you need a commercial license or support contact us at [contact@connect2x.de](mailto:contact@connect2x.de).
