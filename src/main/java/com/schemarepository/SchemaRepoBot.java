package com.schemarepository;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.schemarepository.model.AuditRecord;
import com.schemarepository.model.FeedbackRecord;
import com.schemarepository.model.Manual;
import com.schemarepository.model.User;
import com.schemarepository.repository.JpaAuditRepository;
import com.schemarepository.repository.JpaFeedbackRepository;
import com.schemarepository.repository.JpaManualsRepository;
import com.schemarepository.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberBanned;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.File;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaRepoBot extends TelegramLongPollingBot {
    private final JpaUserRepository userRepository;
    private final JpaAuditRepository auditRepository;
    private final JpaFeedbackRepository feedbackRepository;
    private final JpaManualsRepository manualsRepository;

    @Value("${bot.name}")
    private String botUsername;

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.adminChatIds}")
    private String adminChatIds;

    @Value("${bot.recordsOnPage}")
    private String recordsOnPage;

    private static final LoadingCache<Long, UserSession> userSessionCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .expireAfterAccess(Duration.ofMinutes(10))
            .build(new CacheLoader<>() {
                       @Override
                       public UserSession load(Long s) throws Exception {
                           return null;
                       }
                   }
            );

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
//        log.debug("onUpdateReceived: " + update.toString());
        if (update.hasMessage()) {
            handleMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
        } else {
            handleChatMemberUpdate(update);
        }
    }

    @SneakyThrows
    private void handleMessage(Message message) {
        Optional<User> user = userRepository.getByChatId(message.getChatId());
        if (!user.isPresent()) {
            userRepository.save(new User(message.getFrom().getId(), message.getFrom().getUserName(), message.getFrom().getFirstName(), message.getFrom().getLastName(), null, null, Calendar.getInstance(), null, "active"));
        }
        if (message.hasText() && message.hasEntities()) {
            Optional<MessageEntity> commandEntity = message.getEntities().stream().filter(e -> e.getType().equals("bot_command")).findFirst();
            if (commandEntity.isPresent()) {
                Message messageForUser;
                UserSession userSession = new UserSession();
                userSessionCache.put(message.getChatId(), userSession);
                String command = message.getText().substring(commandEntity.get().getOffset(), commandEntity.get().getLength());
                switch (command) {
                    case "/start":
                        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder inlineKeyboardMarkupBuilder = InlineKeyboardMarkup.builder();
                        addKeyboardRows(null, null, null, 0, inlineKeyboardMarkupBuilder, userSession.getCallbackDataMap());

                        execute(SendMessage.builder()
                                .chatId(message.getChatId())
                                .text("Greetings, I am a bot guide to service manuals and equipment diagrams since 2019.")
                                .build());

                        messageForUser = execute(SendMessage.builder()
                                .chatId(message.getChatId())
                                .text("Choose a section. Enter a substring to search")
                                .parseMode(ParseMode.HTML)
                                .replyMarkup(inlineKeyboardMarkupBuilder.build())
                                .build());

                        userSession.setMessageForUserId(messageForUser.getMessageId());
                        break;
                    case "/registration":
                        messageForUser = execute(SendMessage.builder()
                                .chatId(message.getChatId())
                                .text("To complete, click the \"Register\" button at the bottom of the screen")
                                .replyMarkup(ReplyKeyboardMarkup.builder()
                                        .keyboardRow(new KeyboardRow(List.of(KeyboardButton.builder()
                                                .text("Register")
                                                .requestContact(true)
                                                .build())))
                                        .selective(true)
                                        .resizeKeyboard(true)
                                        .oneTimeKeyboard(true)
                                        .build())
                                .build());

                        userSession.setMessageForUserId(messageForUser.getMessageId());
                        break;

                    case "/wishes":
                        execute(SendMessage.builder()
                                .chatId(message.getChatId())
                                .text("Leave your wish")
                                .build());

                        userSession.setIsWaitFeedback(true);
                        break;
                }
            }
        } else if (message.hasText()) {
            UserSession userSession = userSessionCache.getIfPresent(message.getChatId());
            String messageText = message.getText();
            if (!messageText.isEmpty()) {
                String text;
                if (userSession == null) {
                    execute(SendMessage.builder()
                            .chatId(message.getChatId())
                            .text("Your session has expired, please start again")
                            .build());
                    return;
                }

                if (userSession.getIsWaitFeedback()) {
                    userSession.setIsWaitFeedback(false);

                    feedbackRepository.saveAndFlush(new FeedbackRecord(Calendar.getInstance(), message.getText(), user.get()));

                    execute(SendMessage.builder()
                            .chatId(message.getChatId())
                            .text("Your wish has been saved.")
                            .build());

                    sendMessageToAdmins("New wish from user\n" + message.getText(), user.get());

                    return;
                }

                InlineKeyboardMarkup.InlineKeyboardMarkupBuilder inlineKeyboardMarkupBuilder = InlineKeyboardMarkup.builder();
                Map<UUID, CallbackData> callbackDataMap = userSession.getCallbackDataMap();

                if (userSession.getSelectedType() != null && userSession.getSelectedBrand() != null) {
                    addKeyboardRows(userSession.getSelectedType(), userSession.getSelectedBrand(), messageText, 0, inlineKeyboardMarkupBuilder, callbackDataMap);
                    text = "<b><i>" + userSession.getSelectedType() + " " + userSession.getSelectedBrand() + "</i></b>\nSearch results <i>" + messageText + "</i> in model names";
                } else if (userSession.getSelectedType() != null) {
                    addKeyboardRows(userSession.getSelectedType(), null, messageText, 0, inlineKeyboardMarkupBuilder, callbackDataMap);
                    text = "<b><i>" + userSession.getSelectedType() + "</i></b>\nSearch results <i>" + messageText + "</i> in brand names";
                } else {
                    addKeyboardRows(null, null,  messageText, 0, inlineKeyboardMarkupBuilder, callbackDataMap);
                    text = "Search results <i>" + messageText + "</i> in section titles";
                }

                execute(DeleteMessage.builder()
                        .chatId(message.getChatId())
                        .messageId(message.getMessageId())
                        .build());

                execute(EditMessageText.builder()
                        .chatId(message.getChatId())
                        .messageId(userSession.getMessageForUserId())
                        .parseMode(ParseMode.HTML)
                        .replyMarkup(inlineKeyboardMarkupBuilder.build())
                        .text(text)
                        .build());
            }
        }

        if (message.hasContact()) {
            Contact contact = message.getContact();
            String phoneNumber = contact.getPhoneNumber();
//            Long userId = contact.getUserId();
            String vCard = contact.getVCard();
            String firstName = contact.getFirstName();
            String lastName = contact.getLastName();

            if (user.get().getPhoneNumber() != null) {
                execute(SendMessage.builder()
                        .chatId(message.getChatId())
                        .text("You are already registered")
                        .build());
            } else {
                user.get().setFirstName(firstName);
                user.get().setLastName(lastName);
                user.get().setVCard(vCard);
                user.get().setRegisteredTime(Calendar.getInstance());
                user.get().setPhoneNumber(phoneNumber);

                userRepository.saveAndFlush(user.get());

                execute(SendMessage.builder()
                        .chatId(message.getChatId())
                        .text("Successful registration")
                        .build());

                sendMessageToAdmins("New User Registration", user.get());
            }

            execute(DeleteMessage.builder()
                    .chatId(message.getChatId())
                    .messageId(userSessionCache.get(message.getChatId()).getMessageForUserId())
                    .build());

            userSessionCache.get(message.getChatId()).setMessageForUserId(null);
        }
    }

    @SneakyThrows
    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        Message message = callbackQuery.getMessage();

        UserSession userSession = userSessionCache.getIfPresent(message.getChatId());
        if (userSession == null) {
            execute(SendMessage.builder()
                    .chatId(message.getChatId())
                    .text("Your session has expired, please start again")
                    .build());
            return;
        }

        Map<UUID, CallbackData> callbackDataMap = userSession.getCallbackDataMap();
        CallbackData callbackData = callbackDataMap.get(UUID.fromString(callbackQuery.getData()));
        String filePath = callbackData.getFilePath();
        String type = callbackData.getType();
        String brand = callbackData.getBrand();
        String searchString = callbackData.getSearchString();
        Integer pageNumber = callbackData.getPageNumber();

        userSession.setSelectedType(type);
        userSession.setSelectedBrand(brand);

        if (filePath != null) {
            Optional<User> user = userRepository.getByChatId(message.getChatId());
            if (user.isPresent() && user.get().getPhoneNumber() != null) {
                File file = new File(filePath);
                execute(SendDocument.builder()
                        .chatId(message.getChatId())
                        .document(new InputFile(file))
                        .build());

                auditRepository.save(new AuditRecord(Calendar.getInstance(), "File downloaded " + filePath, user.get()));
            } else {
                execute(SendMessage.builder()
                        .chatId(message.getChatId())
                        .text("Only registered users can download files")
                        .build());

//                auditRepository.save(new AuditRecord(Calendar.getInstance(), "File downloaded " + filePath, user.get()));
            }
        } else {
            String text;
            InlineKeyboardMarkup.InlineKeyboardMarkupBuilder inlineKeyboardMarkupBuilder = InlineKeyboardMarkup.builder();

            if (type != null && brand != null) {
                addKeyboardRows(type, brand, searchString, pageNumber, inlineKeyboardMarkupBuilder, callbackDataMap);
                if (searchString != null) {
                    text = "<b><i>" + type + " " + brand + "</i></b>\nSearch results <i>" + searchString + "</i> in model names";
                } else {
                    text = "<b><i>" + type + " " + brand + "</i></b>";
                }
            } else if (type != null) {
                addKeyboardRows(type, null, searchString, pageNumber, inlineKeyboardMarkupBuilder, callbackDataMap);
                if (searchString != null) {
                    text = "<b><i>" + type + "</i></b>\nSearch results <i>" + searchString + "</i> in brand names";
                } else {
                    text = "<b><i>" + type + "</i></b>";
                }
            } else {
                addKeyboardRows(null, null, searchString, pageNumber, inlineKeyboardMarkupBuilder, callbackDataMap);
                if (searchString != null) {
                    text = "Search results <i>" + searchString + "</i> in section titles";
                } else {
                    text = "Choose a section. Enter a substring to search";
                }
            }

            execute(EditMessageText.builder()
                    .chatId(message.getChatId())
                    .messageId(message.getMessageId())
                    .parseMode(ParseMode.HTML)
                    .replyMarkup(inlineKeyboardMarkupBuilder.build())
                    .text(text)
                    .build());
        }
    }

    @SneakyThrows
    private void handleChatMemberUpdate(Update update) {
        boolean isStopped = false;
        ChatMemberUpdated myChatMember = update.getMyChatMember();
        if (myChatMember.getNewChatMember() instanceof ChatMemberMember /*myChatMember.getNewChatMember().getStatus().equals("member")*/) {
            isStopped = false;
        } else if (myChatMember.getNewChatMember() instanceof ChatMemberBanned /*myChatMember.getNewChatMember().getStatus().equals("kicked")*/) {
            isStopped = true;
        }

        Optional<User> user = userRepository.getByChatId(update.getMyChatMember().getChat().getId());
        if (user.isPresent()) {
            user.get().setStatus(isStopped ? "stopped" : "active");
            userRepository.saveAndFlush(user.get());

            sendMessageToAdmins(isStopped ? "The user stopped the bot" : "The user restarted the bot", user.get());
        }
    }

    @SneakyThrows
    private void sendMessageToAdmins(String message, User user) {
        for (String adminChatId : adminChatIds.split(",")) {
            execute(SendMessage.builder()
                    .chatId(adminChatId)
                    .text(message + "\n" +
                            (user.getUserName() != null ? user.getUserName() + " " : "") +
                            (user.getFirstName() != null ? user.getFirstName() + " " : "") +
                            (user.getLastName() != null ? user.getLastName() + " " : "") +
                            (user.getPhoneNumber() != null ? "(" + user.getPhoneNumber() + ")" : ""))
                    .build());
        }
    }

    private void addKeyboardRows(String type, String brand, String searchString, Integer pageNum, InlineKeyboardMarkup.InlineKeyboardMarkupBuilder builder, Map<UUID, CallbackData> callbackDataMap) {
        callbackDataMap.clear();

        Page<Manual> page = getPage(type, brand, searchString, pageNum);
        for (Manual manual : page.getContent()) {
            builder.keyboardRow(List.of(InlineKeyboardButton.builder()
                    .text((type != null && brand != null ? manual.getModelName() : (type != null ? manual.getBrand().getName() : manual.getType().getName())))
                    .callbackData(prepareAndSaveCallbackData(
                            type != null && brand != null ? manual.getFilePath() : null,
                            brand == null ? manual.getType().getName() : null,
                            type != null && brand == null ? manual.getBrand().getName() : null,
                            null,
                            type == null || brand == null ? 0 : null,
                            callbackDataMap).toString())
                    .build()));
        }

        if (page.getTotalPages() > 1) {
            builder.keyboardRow(List.of(
                    InlineKeyboardButton.builder()
                            .text("⏪")
                            .callbackData(prepareAndSaveCallbackData(
                                    null,
                                    type != null ? type : null,
                                    brand != null ? brand : null,
                                    searchString != null ? searchString : null,
                                    0,
                                    callbackDataMap).toString())
                            .build(),
                    InlineKeyboardButton.builder()
                            .text("◀")
                            .callbackData(prepareAndSaveCallbackData(
                                    null,
                                    type != null ? type : null,
                                    brand != null ? brand : null,
                                    searchString != null ? searchString : null,
                                    pageNum == 0 ? pageNum : pageNum - 1,
                                    callbackDataMap).toString())
                            .build(),
                    InlineKeyboardButton.builder()
                            .text(pageNum + 1 + " from " + page.getTotalPages())
                            .callbackData(prepareAndSaveCallbackData(
                                    null,
                                    type != null ? type : null,
                                    brand != null ? brand : null,
                                    searchString != null ? searchString : null,
                                    pageNum,
                                    callbackDataMap).toString())
                            .build(),
                    InlineKeyboardButton.builder()
                            .text("▶")
                            .callbackData(prepareAndSaveCallbackData(
                                    null,
                                    type != null ? type : null,
                                    brand != null ? brand : null,
                                    searchString != null ? searchString : null,
                                    pageNum == page.getTotalPages() - 1 ? pageNum : pageNum + 1,
                                    callbackDataMap).toString())
                            .build(),
                    InlineKeyboardButton.builder()
                            .text("⏩")
                            .callbackData(prepareAndSaveCallbackData(
                                    null,
                                    type != null ? type : null,
                                    brand != null ? brand : null,
                                    searchString != null ? searchString : null,
                                    page.getTotalPages() - 1,
                                    callbackDataMap).toString())
                            .build()
            ));
        }

        if (type != null || searchString != null) {
            builder.keyboardRow(List.of(
                    InlineKeyboardButton.builder()
                            .text("⬅")
                            .callbackData(prepareAndSaveCallbackData(
                                    null,
                                    type != null && brand != null ? type : null,
                                    null,
                                    null,
                                    0,
                                    callbackDataMap).toString())
                            .build()
            ));
        }
    }

    private Page<Manual> getPage(String type, String brand, String searchString, Integer pageNum) {
        Page<Manual> page;
        if (type != null && brand != null) {
            if (searchString != null) {
                page = manualsRepository.findAllModelByTypeAndBrandLike(type, brand, searchString.toLowerCase(), PageRequest.of(pageNum, Integer.valueOf(recordsOnPage)));
            } else {
                page = manualsRepository.findAllModelByTypeAndBrand(type, brand, PageRequest.of(pageNum, Integer.valueOf(recordsOnPage)));
            }
        } else if (type != null) {
            if (searchString != null) {
                page = manualsRepository.findAllBrandByTypeLike(type, searchString.toLowerCase(), PageRequest.of(pageNum, Integer.valueOf(recordsOnPage)));
            } else {
                page = manualsRepository.findAllBrandByType(type, PageRequest.of(pageNum, Integer.valueOf(recordsOnPage)));
            }
        } else {
            if (searchString != null) {
                page = manualsRepository.findAllTypeLike(searchString.toLowerCase(), PageRequest.of(pageNum, Integer.valueOf(recordsOnPage)));
            } else {
                page = manualsRepository.findAllType(PageRequest.of(pageNum, Integer.valueOf(recordsOnPage)));
            }
        }
        return page;
    }

    private UUID prepareAndSaveCallbackData(String filePath, String type, String brand, String searchString, Integer pageNumber, Map<UUID, CallbackData> callbackDataMap) {
        CallbackData callbackData = new CallbackData(filePath, type, brand, searchString, pageNumber);

        UUID uuid = UUID.nameUUIDFromBytes(callbackData.toString().getBytes());
        callbackDataMap.put(uuid, callbackData);

        return uuid;
    }
}
