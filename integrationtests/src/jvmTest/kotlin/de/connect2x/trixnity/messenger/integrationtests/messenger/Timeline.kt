package de.connect2x.trixnity.messenger.integrationtests.messenger

import de.connect2x.lognity.api.logger.Logger
import de.connect2x.trixnity.messenger.integrationtests.util.waitFor
import de.connect2x.trixnity.messenger.viewmodel.RootRouter
import de.connect2x.trixnity.messenger.viewmodel.RootViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.RoomRouter
import de.connect2x.trixnity.messenger.viewmodel.room.RoomViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.settings.ExtrasRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineRouter
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.TimelineViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.BaseTimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementHolderViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.TimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.TextBased
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.RoomMessageTimelineElementViewModel.VerificationRequest
import de.connect2x.trixnity.messenger.viewmodel.room.timeline.elements.message.VerificationDoneTimelineElementViewModel
import de.connect2x.trixnity.messenger.viewmodel.roomlist.RoomListRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationRouter
import de.connect2x.trixnity.messenger.viewmodel.verification.VerificationViewModel
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import de.connect2x.trixnity.core.model.RoomId
import de.connect2x.trixnity.core.model.UserId
import kotlin.time.Duration.Companion.seconds

private val log: Logger = Logger("de.connect2x.trixnity.messenger.integrationtests.messenger.TimelineKt")

suspend fun MatrixMessengerWithRoot.sendMessage(roomId: RoomId, message: String) = with(root) {
    withTimeout(20.seconds) {
        val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
        val roomListViewModel =
            mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel
        roomListViewModel.selectRoom(roomId)
        val roomViewModel = mainViewModel.roomRouterStack.waitFor(RoomRouter.Wrapper.View::class).viewModel
        val timelineViewModel = roomViewModel.timelineStack.waitFor(TimelineRouter.Wrapper.View::class).viewModel
        val inputAreaViewModel = timelineViewModel.inputAreaViewModel
        val job = launch { inputAreaViewModel.isSendEnabled.collect {} }
        inputAreaViewModel.textField.update(message)
        log.debug { "wait for send to be enabled" }
        inputAreaViewModel.isSendEnabled.first { it }
        inputAreaViewModel.sendMessage()
        timelineViewModel.jumpToEndOfTimeline() // TODO remove?
        findTimelineElement<TimelineElementHolderViewModel>(roomViewModel) { vm ->
            vm is TextBased.Text && vm.body == message
        }
        job.cancel()
    }
}

suspend fun MatrixMessengerWithRoot.findMessage(roomId: RoomId, message: String): Boolean = with(root) {
    withTimeoutOrNull(10.seconds) {
        val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
        val roomListViewModel =
            mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel
        roomListViewModel.selectRoom(roomId)
        val roomViewModel = mainViewModel.roomRouterStack.waitFor(RoomRouter.Wrapper.View::class).viewModel
        findTimelineElement<TimelineElementHolderViewModel>(roomViewModel) { vm ->
            vm is TextBased.Text && vm.body == message
        }
        true
    } ?: false
}

suspend fun MatrixMessengerWithRoot.initiateUserVerification(roomId: RoomId, userId: UserId) = with(root) {
    log.info { "  +- initiate user verification (client1)" }
    withTimeout(20.seconds) {
        val roomViewModel = goToRoom(roomId)
        roomViewModel.openUserProfile(userId)
        val userProfileViewModel =
            roomViewModel.extrasStack.waitFor(ExtrasRouter.Wrapper.UserProfile::class).viewModel
        withTimeout(5.seconds) { userProfileViewModel.canVerifyUser.first { it } }
        userProfileViewModel.startVerification()
        log.debug { "started user verification" }
        delay(1.seconds) // wait for request to finish
        val verificationRequest = findActiveVerification(roomViewModel)
        log.debug { "wait for verification state machine to move on" }
        val verificationViewModel =
            verificationRequest.stack.waitFor(VerificationRouter.Wrapper.Verification::class).viewModel
        verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.Wait::class)
        log.debug { "user verification started" }
    }
}

suspend fun MatrixMessengerWithRoot.acceptUserVerification(roomId: RoomId, otherUserId: UserId) = with(root) {
    log.info { "  +- accept user verification (client2)" }
    withTimeout(20.seconds) {
        val verificationViewModel = verificationViewModel(roomId)
        val requestViewModel =
            verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.Request::class).viewModel
        requestViewModel.theirUserId shouldBe otherUserId
        requestViewModel.next()
        log.debug { "user verification accepted" }
    }
}

suspend fun MatrixMessengerWithRoot.startVerificationWithEmoji(roomId: RoomId) = with(root) {
    log.info { "  +- start verification with emoji (client1)" }
    withTimeout(20.seconds) {
        val verificationViewModel = verificationViewModel(roomId)
        val selectVerification =
            verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.SelectVerificationMethod::class).viewModel
        selectVerification.verificationMethods.size shouldBe 1
        selectVerification.acceptVerificationMethod(selectVerification.verificationMethods[0].first)
        verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.Wait::class)
    }
}

suspend fun MatrixMessengerWithRoot.acceptVerificationWithEmoji(roomId: RoomId) = with(root) {
    log.info { "  +- accept verification with emoji (client2)" }
    withTimeout(20.seconds) {
        val verificationViewModel = verificationViewModel(roomId)
        val acceptSasStartViewModel =
            verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.AcceptSasStart::class).viewModel
        acceptSasStartViewModel.accept()
        val compareViewModel =
            verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.CompareEmojisOrNumbers::class).viewModel
        compareViewModel.accept()
        verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.Wait::class)
    }
}

suspend fun MatrixMessengerWithRoot.originalClientAcceptVerificationWithEmoji(roomId: RoomId) = with(root) {
    log.info { "  +- other client accept verification with emoji (client1)" }
    val verificationViewModel = verificationViewModel(roomId)
    val compareViewModel =
        verificationViewModel.stack.waitFor(VerificationViewModel.Wrapper.CompareEmojisOrNumbers::class).viewModel
    compareViewModel.accept()
    val roomViewModel = goToRoom(roomId)
    val done =
        findTimelineElement<VerificationDoneTimelineElementViewModel, BaseTimelineElementHolderViewModel>(
            roomViewModel
        )
    done.message shouldBe "Successful"
}

private suspend fun RootViewModel.goToRoom(roomId: RoomId): RoomViewModel {
    log.debug { "go to room $roomId" }
    val mainViewModel = stack.waitFor(RootRouter.Wrapper.Main::class).viewModel
    mainViewModel.roomListRouterStack.waitFor(RoomListRouter.Wrapper.List::class).viewModel.selectRoom(roomId)
    val roomViewModel = mainViewModel.roomRouterStack.waitFor(RoomRouter.Wrapper.View::class).viewModel
    log.debug { "go to room $roomId successful" }
    return roomViewModel
}

private suspend fun RootViewModel.verificationViewModel(roomId: RoomId): VerificationViewModel {
    val roomViewModel = goToRoom(roomId)
    val verificationRequest = findActiveVerification(roomViewModel)
    val verificationViewModel =
        verificationRequest.stack.waitFor(VerificationRouter.Wrapper.Verification::class).viewModel
    return verificationViewModel
}

private suspend fun findActiveVerification(roomViewModel: RoomViewModel): VerificationRequest {
    log.debug { "find active verification" }
    val verificationRequest =
        findTimelineElement<VerificationRequest, BaseTimelineElementHolderViewModel>(roomViewModel)
    log.debug { "found active verification: $verificationRequest" }
    return verificationRequest
}

private suspend inline fun <reified H : BaseTimelineElementHolderViewModel> findTimelineElement(
    roomViewModel: RoomViewModel,
    crossinline condition: (TimelineElementViewModel<*>) -> Boolean,
) {
    val timelineViewModel = roomViewModel.timelineStack.waitFor(TimelineRouter.Wrapper.View::class).viewModel
    timelineViewModel.elements.first { vms ->
        log.debug { "vms: $vms" }
        initViewState(timelineViewModel, vms)
        vms
            .filterIsInstance<H>()
            .any {
                val vm = it.element.filterNotNull().first()
                log.trace { "+++ vm: $vm, ${vm::class.simpleName}" }
                condition(vm)
            }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend inline fun <reified T : TimelineElementViewModel<*>, reified H : BaseTimelineElementHolderViewModel> findTimelineElement(
    roomViewModel: RoomViewModel,
): T {
    val timelineViewModel = roomViewModel.timelineStack.waitFor(TimelineRouter.Wrapper.View::class).viewModel
    timelineViewModel.elements.first { it.isNotEmpty() }
    timelineViewModel.elements.flatMapLatest { vms ->
        initViewState(timelineViewModel, vms)
        combine(
            vms
                .filterIsInstance<H>()
                .map { it.element.filterNotNull() }) {
            it.toList()
        }
    }.first { vms ->
        log.debug { "+++ vms: $vms" }
        vms
            .any { vm ->
                log.debug { "+++ vm: ${vm::class.simpleName} (is ${vm is T})" }
                vm is T
            }
    }

    return timelineViewModel.elements.map {
        it
            .filterIsInstance<H>()
            .find { vm -> vm.element.filterNotNull().first() is T }
    }.filterNotNull().first().element.filterNotNull().first() as T
}

private fun initViewState(
    timelineViewModel: TimelineViewModel,
    vms: List<BaseTimelineElementHolderViewModel>
) {
    if (timelineViewModel.viewState.value == null) {
        timelineViewModel.viewState.value = TimelineViewModel.ViewState(
            firstVisibleElement = vms.first().key,
            lastVisibleElement = vms.last().key,
            firstLoadedElement = vms.first().key,
            lastLoadedElement = vms.last().key,
            timelineIsFocused = true,
        )
    }
}
