# Trixnity Messenger - A headless Matrix messenger

Trixnity Messenger provides extensions on top of [Trixnity](https://gitlab.com/trixnity/trixnity)
geared towards building a multiplatform messenger. It is agnostic to the UI and supports all technologies that are interoperable with
Kotlin. UI frameworks that are reactive like
[Compose Multiplatform](https://www.jetbrains.com/lp/compose-mpp), [SwiftUI](https://developer.apple.com/xcode/swiftui/)
, or [ReactJS](https://reactjs.org/) are best suited, since the changes in Trixnity Messenger can be reflected in the
component by binding to the view model.

**You need help? Ask your questions in [#trixnity-messenger:imbitbu.de](https://matrix.to/#/#trixnity-messenger:imbitbu.de).**

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

In order to use the SDK, you have to create some objects. Those are:
* a `KoinApplication` (see [DI](#change-the-default-behavior-of-view-models)) that looks like this
  ```kotlin
  val koinApplication = koinApplication {
    modules(trixnityMessengerModule())
  }
  ```
* a `MatrixClientService` (holds `MatrixClient`s that can be used to access Matrix APIs, see [Trixnity](https://gitlab.com/trixnity/trixnity)):
  ```kotlin
     val matrixClientService = DefaultMatrixClientService(koinApplication.koin)
  ```
* a `ComponentContext` (from [Decompose](https://github.com/arkivanov/Decompose)):
  ```kotlin
  val componentContext = DefaultComponentContext(LifecycleRegistry())
  ```

Then you are ready to create the root node of the view model tree that is used in your app.
```kotlin
val rootViewModel = RootViewModelImpl(
  componentContext = componentContext,
  matrixClientService = matrixClientService,
  initialSyncOnceIsFinished = { }, // not needed in our small example
  koinApplication = koinApplication,
)
```

Create a root node in your UI framework and pass the created `rootViewModel` to it. In Compose Multiplatform on the desktop, it looks something like this:
```kotlin
application {
  Window(...) {
    MyMatrixClient(rootViewModel)
  }
}
```
where `MyMatrixClient` is a `@Composable` function that gets the `RootViewModel` as a parameter.

Now you are ready to react to different states of the routing in the `RootViewModel`.

### Routing
The `RootViewModel` itself does not do much on its own, but is a point where routing kicks in. Different views in the view models are organized in stacks that show one view on top and possibly some views behind the top stack (see [Decompose routing](https://arkivanov.github.io/Decompose/navigation/overview/)).

In our case, let's have a look at `rootViewModel.rootStack`. It returns a `Value<ChildStack<RootRouter.Config, RootRouter.RootWrapper>>`, i.e. a value changing over time that is providing the UI with an instance of `RootRouter.RootWrapper`. In a first step, let's observe this value and react to changes:
```kotlin
// this code has to be called from a `suspend` function
rootViewModel.rootStack.toFlow()
  .mapLatest { it.active.instance }
  .collect { wrapper ->
    when(wrapper) {
      is RootRouter.RootWrapper.None -> {} // draw an empty UI
      is RootRouter.RootWrapper.MatrixClientInitialization -> {} // show initialization of the MatrixClient (aka loading screen)
      else -> {} // add more cases    
    } 
  }
```

In case you are using Compose as your UI framework, Decompose has some [helpers](https://arkivanov.github.io/Decompose/extensions/compose/#navigating-between-composable-components) for routing.

## Configuration
Trixnity Messenger has multiple ways to configure the client to your needs.

### MessengerConfig
The class `MessengerConfig` contains static information that is used to determine some folder names and other data in the lifecycle of the messenger. To override the standard configuration, put this in your code:
```kotlin
val messengerConfig: MessengerConfig.() -> Unit = {
  appName = "MyMatrixClient"
  // more things you would like to change
}

// before a view model is created
MessengerConfig.instance.apply(messengerConfig)
```

### Change the default behavior of view models
You can customize the messenger SDK to fit your needs with the help of dependency injection (DI). Trixnity Messenger uses [Koin](https://insert-koin.io/) for this.

Suppose you want to deliver a demo version of your messenger and with it, want to fix the server url when the client tries to login the user to a Matrix server. To do this, you have to do the following:
* provide an alternative implementation to a view model interface, here `AddMatrixAccountViewModel`
```kotlin
  class MyAddMatrixAccountViewModel(
    viewModelContext: ViewModelContext,
    addMatrixAccountViewModel: AddMatrixAccountViewModelImpl,
  ): ViewModelContext by viewModelContext, AddMatrixAccountViewModel by addMatrixAccountViewModel {
    
    private val isDemoVersion: Boolean = ... // this is computed from the config or a runtime parameter
    val canChangeServerUrl: Boolean = !isDemoVersion
    override val serverUrl: MutableStateFlow<String> = MutableStateFlow(if (isDemoVersion) "https://myUrl" else addMatrixAccountViewModel.serverUrl.value)
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

When you start your application with this configuration, the implementation of `AddMatrixAccountViewModel` will be your customized version `MyAddMatrixAccountViewModel` and your UI can use all the properties and methods of it (maybe a downcast from the `AddMatrixViewModel` interface is needed).

### i18n

Trixnity Messenger comes with a set of standard translations for some states that can occur. It currently supports
English (en) and German (de). It uses a simple
[Kotlin file](./trixnity-messenger/src/commonMain/kotlin/de/connect2x/trixnity/messenger/util/I18n.kt) for all translations.

It allows the same customizations as view models. In order to change messages, simply override the messages you want to change by subclassing [I18nBase](./trixnity-messenger/src/commonMain/kotlin/de/connect2x/trixnity/messenger/util/I18n.kt).

If you want to add new messages, use the delegation pattern as described in [View model customization](#change-the-default-behavior-of-view-models) and add more messages.


## Commercial license and support
If you need a commercial license or support contact us at [kontakt@connect2x.de](mailto:kontakt@connect2x.de).
