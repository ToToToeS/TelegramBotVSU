package sia.telegramvsu.service;

import jakarta.ws.rs.NotFoundException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import sia.telegramvsu.config.BotConfig;
import sia.telegramvsu.model.NumberLesson;
import sia.telegramvsu.model.User;
import sia.telegramvsu.model.UserRepository;
import sia.telegramvsu.model.WeekDay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Getter
@Setter
@EnableScheduling
public class TelegramBot extends TelegramLongPollingBot {

    private final String TEACHER = "Преподаватель";
    private final String STUDENT = "Cтудент";
    private final String NOBODY = "Никто";

    private UserRepository userRepository;
    private BotConfig botConfig;
    private ExcelParser excelParser;
    private DownloadExcel downloadExcel;
    private WeekDay dayLesson;

    @Scheduled(cron = "0 0 6 * * *")
    public void downloadExcel() throws IOException {
       downloadExcel.downloadSchedules();
       excelParser.parseExel();
    }

    @Autowired
    public TelegramBot(@Value("${path.excel}") String exelPath, BotConfig botConfig,
                       UserRepository userRepository, DownloadExcel downloadExcel, ExcelParser excelParser) throws IOException {
        this.excelParser = excelParser;
        this.botConfig = botConfig;
        this.userRepository = userRepository;
        this.downloadExcel = downloadExcel;

        downloadExcel.downloadSchedules();
        excelParser.parseExel();

    }

    @Override
    public String getBotUsername() {return botConfig.getBotName();}

    @Override
    public String getBotToken() {return botConfig.getToken();}

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {

            long chatId = update.getMessage().getChatId();
            Message msg = update.getMessage();
            User user = userRepository.findById(chatId).orElse(new User());

            if (user.getUserName() == null ) {
                registerUsersInDB(msg);
                sendChosenStatus(chatId);
            }

            if (user.getUserName() != null) {
                switch (msg.getText()) {
                    case "/reset":
                            user.setGroup(null);
                            user.setStatus(NOBODY);
                            userRepository.save(user);
                            sendChosenStatus(chatId);

                        return;
                    case "/donate":
                        sendMessage(chatId, """
                                belinvestbank: 5578843371248679
                                """);
                        return;
                    case "/free":
                         sendChosenDayWeekForSearchLesson(chatId);
                }

            }


            if (user.getStatus().equals(TEACHER) && user.getGroup() == null) {
                if (excelParser.getTeacherHowInSchedule(msg.getText()) != null) {
                    user.setGroup(excelParser.getTeacherHowInSchedule(msg.getText()));
                    sendChosenDayWeek(chatId, msg.getText());
                    userRepository.save(user);
                    return;
                } else {
                    sendMessage(chatId,"Введите ФИО так как указанно в расписании \nНапример: Дрозд Е. М.");
                    return;
                }
            }

            if (user.getStatus().equals(STUDENT) && user.getGroup() == null) {
                if (excelParser.getGroupHowInSchedule(msg.getText()) != null) {
                    user.setGroup(excelParser.getGroupHowInSchedule(msg.getText()));
                    sendChosenDayWeek(chatId, msg.getText());
                    userRepository.save(user);
                    return;
                } else {
                    sendMessage(chatId,"Введите название группы вместе с подгруппой так как указанно в расписании \nНапример: 24ИСиТ1д_1");
                    return;
                }
            }



        } else if (update.hasCallbackQuery()) {
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            String callBackQuery = update.getCallbackQuery().getData();
            User user = userRepository.findById(chatId).orElseThrow(() -> new NotFoundException("user not found with id " + chatId));
            String group = user.getGroup();

            if (callBackQuery.equals("MONDAY_BUTTON")) {

                String message = user.getStatus().equals(TEACHER) ? excelParser.getDaySubjectsTeacher(WeekDay.MONDAY, group) : excelParser.getDaySubjectsStudent(WeekDay.MONDAY, group);
                sendSchedules(chatId, message);
                deleteMessages(chatId, messageId);

            } else if (callBackQuery.equals("TUESDAY_BUTTON")) {

                String message = user.getStatus().equals(TEACHER) ? excelParser.getDaySubjectsTeacher(WeekDay.TUESDAY, group) : excelParser.getDaySubjectsStudent(WeekDay.TUESDAY, group);
                sendSchedules(chatId, message);
                deleteMessages(chatId, messageId);

            } else if (callBackQuery.equals("WEDNESDAY_BUTTON")) {

                String message = user.getStatus().equals(TEACHER) ? excelParser.getDaySubjectsTeacher(WeekDay.WEDNESDAY, group) : excelParser.getDaySubjectsStudent(WeekDay.WEDNESDAY, group);
                sendSchedules(chatId, message);
                deleteMessages(chatId, messageId);

            }else if (callBackQuery.equals("THURSDAY_BUTTON")) {

                String message = user.getStatus().equals(TEACHER) ? excelParser.getDaySubjectsTeacher(WeekDay.THURSDAY, group) : excelParser.getDaySubjectsStudent(WeekDay.THURSDAY, group);
                sendSchedules(chatId, message);
                deleteMessages(chatId, messageId);

            } else if (callBackQuery.equals("FRIDAY_BUTTON")) {

                String message = user.getStatus().equals(TEACHER) ? excelParser.getDaySubjectsTeacher(WeekDay.FRIDAY, group) : excelParser.getDaySubjectsStudent(WeekDay.FRIDAY, group);
                sendSchedules(chatId, message);
                deleteMessages(chatId, messageId);

            } else if (callBackQuery.equals("SATURDAY_BUTTON")) {

                String message = user.getStatus().equals(TEACHER) ? excelParser.getDaySubjectsTeacher(WeekDay.SATURDAY, group) : excelParser.getDaySubjectsStudent(WeekDay.SATURDAY, group);
                sendSchedules(chatId, message);
                deleteMessages(chatId, messageId);

            } else if (callBackQuery.equals("MONDAY_BUTTON_LESSON")) {
                dayLesson = WeekDay.MONDAY;
                sendChosenLessonNumber(chatId);
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("TUESDAY_BUTTON_LESSON")) {
                dayLesson = WeekDay.TUESDAY;
                sendChosenLessonNumber(chatId);
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("WEDNESDAY_BUTTON_LESSON")) {
                dayLesson = WeekDay.WEDNESDAY;
                sendChosenLessonNumber(chatId);
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("THURSDAY_BUTTON_LESSON")) {
                dayLesson = WeekDay.THURSDAY;
                sendChosenLessonNumber(chatId);
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("FRIDAY_BUTTON_LESSON")) {
                dayLesson = WeekDay.FRIDAY;
                sendChosenLessonNumber(chatId);
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("LESSON_1")) {
                sendMessage(chatId, excelParser.getFreeAuditoriums(dayLesson, NumberLesson.LESSON_1).toString());
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("LESSON_2")) {
                sendMessage(chatId, excelParser.getFreeAuditoriums(dayLesson, NumberLesson.LESSON_2).toString());
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("LESSON_3")) {
                sendMessage(chatId, excelParser.getFreeAuditoriums(dayLesson, NumberLesson.LESSON_3).toString());
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("LESSON_4")) {
                sendMessage(chatId, excelParser.getFreeAuditoriums(dayLesson, NumberLesson.LESSON_4).toString());
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("LESSON_5")) {
                sendMessage(chatId, excelParser.getFreeAuditoriums(dayLesson, NumberLesson.LESSON_5).toString());
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("LESSON_6")) {
                sendMessage(chatId, excelParser.getFreeAuditoriums(dayLesson, NumberLesson.LESSON_6).toString());
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("LESSON_7")) {
                sendMessage(chatId, excelParser.getFreeAuditoriums(dayLesson, NumberLesson.LESSON_7).toString());
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("LESSON_8")) {
                sendMessage(chatId, excelParser.getFreeAuditoriums(dayLesson, NumberLesson.LESSON_8).toString());
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("ALL_BUTTON")) {
                String message = user.getStatus().equals(TEACHER) ? excelParser.getWeekSubjectsTeacher(group) : excelParser.getWeekSubjectsStudent(group);
                sendSchedules(chatId, message);
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("CHANGE_DAY")) {
                sendChosenDayWeek(chatId, user.getGroup());
            } else if (callBackQuery.equals("TEACHER_BUTTON")) {
                sendMessage(chatId, "Введите ФИО так как указанно в расписании \nНапример: Дрозд Е. М.");
                user.setStatus(TEACHER);
                user.setGroup(null);
                userRepository.save(user);
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("STUDENT_BUTTON")) {
                sendMessage(chatId, "Введите название группы вместе с подгруппой так как указанно в расписании \nНапример: 24ИСиТ1д_1");
                user.setStatus(STUDENT);
                user.setGroup(null);
                userRepository.save(user);
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("ALL_BUTTON")) {
                String message = user.getStatus().equals(TEACHER) ? excelParser.getWeekSubjectsTeacher(group) : excelParser.getWeekSubjectsStudent(group);
                sendSchedules(chatId, message);
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("CHANGE_DAY")) {
                sendChosenDayWeek(chatId, user.getGroup());
            } else if (callBackQuery.equals("TEACHER_BUTTON")) {
                sendMessage(chatId, "Введите ФИО так как указанно в расписании \nНапример: Дрозд Е. М.");
                user.setStatus(TEACHER);
                user.setGroup(null);
                userRepository.save(user);
                deleteMessages(chatId, messageId);
            } else if (callBackQuery.equals("STUDENT_BUTTON")) {
                sendMessage(chatId, "Введите название группы вместе с подгруппой так как указанно в расписании \nНапример: 24ИСиТ1д_1");
                user.setStatus(STUDENT);
                user.setGroup(null);
                userRepository.save(user);
                deleteMessages(chatId, messageId);
            }
        }
    }

    private void sendChosenLessonNumber(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("""
                Выберете номер занятия:
                """);

        InlineKeyboardMarkup inlineKeyboardButton = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine2 = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine3 = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine4 = new ArrayList<>();


        var first = new InlineKeyboardButton();
        var second = new InlineKeyboardButton();
        var third = new InlineKeyboardButton();
        var fourth = new InlineKeyboardButton();
        var fifth = new InlineKeyboardButton();
        var sixth = new InlineKeyboardButton();
        var seventh = new InlineKeyboardButton();
        var eight = new InlineKeyboardButton();



        first.setText("1");
        second.setText("2");
        third.setText("3");
        fourth.setText("4");
        fifth.setText("5");
        sixth.setText("6");
        seventh.setText("7");
        eight.setText("8");


        first.setCallbackData("LESSON_1");
        second.setCallbackData("LESSON_2");
        third.setCallbackData("LESSON_3");
        fourth.setCallbackData("LESSON_4");
        fifth.setCallbackData("LESSON_5");
        sixth.setCallbackData("LESSON_6");
        seventh.setCallbackData("LESSON_7");
        eight.setCallbackData("LESSON_8");

        rowInLine1.add(first);
        rowInLine1.add(second);
        rowInLine2.add(third);
        rowInLine2.add(fourth);
        rowInLine3.add(fifth);
        rowInLine3.add(sixth);
        rowInLine4.add(seventh);
        rowInLine4.add(eight);


        rowsInLine.add(rowInLine1);
        rowsInLine.add(rowInLine2);
        rowsInLine.add(rowInLine3);
        rowsInLine.add(rowInLine4);

        inlineKeyboardButton.setKeyboard(rowsInLine);

        sendMessage.setReplyMarkup(inlineKeyboardButton);
        executeMessage(sendMessage);
    }

    private void sendChosenDayWeek(long chatId, String group) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("""
                %s
                
                Выберете день недели:
                """.formatted(group));

        InlineKeyboardMarkup inlineKeyboardButton = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine2 = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine3 = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine4 = new ArrayList<>();


        var monButton = new InlineKeyboardButton();
        var tuyButton = new InlineKeyboardButton();
        var wedButton = new InlineKeyboardButton();
        var thuButton = new InlineKeyboardButton();
        var friButton = new InlineKeyboardButton();
        var satButton = new InlineKeyboardButton();
        var allButton = new InlineKeyboardButton();



        monButton.setText("Понедельник");
        tuyButton.setText("Вторник");
        wedButton.setText("Среда");
        thuButton.setText("Четверг");
        friButton.setText("Пятница");
        satButton.setText("Суббота");
        allButton.setText("Вся неделя");


        monButton.setCallbackData("MONDAY_BUTTON");
        tuyButton.setCallbackData("TUESDAY_BUTTON");
        wedButton.setCallbackData("WEDNESDAY_BUTTON");
        thuButton.setCallbackData("THURSDAY_BUTTON");
        friButton.setCallbackData("FRIDAY_BUTTON");
        satButton.setCallbackData("SATURDAY_BUTTON");
        allButton.setCallbackData("ALL_BUTTON");

        rowInLine1.add(monButton);
        rowInLine1.add(tuyButton);
        rowInLine2.add(wedButton);
        rowInLine2.add(thuButton);
        rowInLine3.add(friButton);
        rowInLine3.add(satButton);
        rowInLine4.add(allButton);


        rowsInLine.add(rowInLine1);
        rowsInLine.add(rowInLine2);
        rowsInLine.add(rowInLine3);
        rowsInLine.add(rowInLine4);

        inlineKeyboardButton.setKeyboard(rowsInLine);

        sendMessage.setReplyMarkup(inlineKeyboardButton);
        executeMessage(sendMessage);
    }

    private void sendChosenDayWeekForSearchLesson(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("""
                Выберете день недели:
                """);

        InlineKeyboardMarkup inlineKeyboardButton = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine2 = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine3 = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine4 = new ArrayList<>();


        var monButton = new InlineKeyboardButton();
        var tuyButton = new InlineKeyboardButton();
        var wedButton = new InlineKeyboardButton();
        var thuButton = new InlineKeyboardButton();
        var friButton = new InlineKeyboardButton();
        var satButton = new InlineKeyboardButton();
        var allButton = new InlineKeyboardButton();



        monButton.setText("Понедельник");
        tuyButton.setText("Вторник");
        wedButton.setText("Среда");
        thuButton.setText("Четверг");
        friButton.setText("Пятница");
        satButton.setText("Суббота");


        monButton.setCallbackData("MONDAY_BUTTON_LESSON");
        tuyButton.setCallbackData("TUESDAY_BUTTON_LESSON");
        wedButton.setCallbackData("WEDNESDAY_BUTTON_LESSON");
        thuButton.setCallbackData("THURSDAY_BUTTON_LESSON");
        friButton.setCallbackData("FRIDAY_BUTTON_LESSON");
        satButton.setCallbackData("SATURDAY_BUTTON_LESSON");

        rowInLine1.add(monButton);
        rowInLine1.add(tuyButton);
        rowInLine2.add(wedButton);
        rowInLine2.add(thuButton);
        rowInLine3.add(friButton);
        rowInLine3.add(satButton);


        rowsInLine.add(rowInLine1);
        rowsInLine.add(rowInLine2);
        rowsInLine.add(rowInLine3);

        inlineKeyboardButton.setKeyboard(rowsInLine);

        sendMessage.setReplyMarkup(inlineKeyboardButton);
        executeMessage(sendMessage);
    }

    private void sendChosenStatus(long chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Кто вы?");
        sendMessage.setChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardButton = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine1 = new ArrayList<>();

        var teacherButton = new InlineKeyboardButton();
        var studentButton = new InlineKeyboardButton();

        teacherButton.setText("Преподаватель");
        studentButton.setText("Студент");

        teacherButton.setCallbackData("TEACHER_BUTTON");
        studentButton.setCallbackData("STUDENT_BUTTON");

        rowInLine1.add(teacherButton);
        rowInLine1.add(studentButton);

        rowsInLine.add(rowInLine1);

        inlineKeyboardButton.setKeyboard(rowsInLine);

        sendMessage.setReplyMarkup(inlineKeyboardButton);
        executeMessage(sendMessage);
    }

    private void sendSchedules(long chatId, String formatDaySchedules) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(formatDaySchedules);
        sendMessage.setParseMode("HTML");

        InlineKeyboardMarkup inlineKeyboardButton = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        var dayButton = new InlineKeyboardButton();

        dayButton.setText("Выбрать другой день");
        dayButton.setCallbackData("CHANGE_DAY");

        rowInLine.add(dayButton);

        rowsInLine.add(rowInLine);

        inlineKeyboardButton.setKeyboard(rowsInLine);

        sendMessage.setReplyMarkup(inlineKeyboardButton);
        executeMessage(sendMessage);
    }

    private void deleteMessages(long chatId, int messageId ) {
        DeleteMessage deleteMessage = new DeleteMessage();

        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);

        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            log.error("Error in time sending message" + e.getMessage());
        }
    }

    private void sendMessage(long chatId, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        sendMessage.setParseMode("HTML");
        executeMessage(sendMessage);
    }

    private void registerUsersInDB(Message msg) {
        var chat = msg.getChat();

        User user = new User();
        user.setId(msg.getChatId());
        user.setFirstName(chat.getFirstName());
        user.setLastName(chat.getLastName());
        user.setUserName(chat.getUserName());
        user.setStatus(NOBODY);

        userRepository.save(user);
        log.info("User register " + user.toString());
    }

    private void executeMessage(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error in time sending message" + e.getMessage());
        }
    }
}
