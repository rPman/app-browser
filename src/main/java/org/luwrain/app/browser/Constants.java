
package org.luwrain.app.browser;

import java.util.*;

interface Constants
{
    static final String PAGE_ANY_STATE_LOADED="Страница загружена";
    static final String PAGE_ANY_STATE_CANCELED="Загрузка страницы отменена";
    static final String PAGE_ANY_SCREENMODE_PAGE="информация о странице";
    static final String PAGE_ANY_SCREENMODE_DOWNLOAD="загрузка файла ";
    static final String PAGE_ANY_SCREENMODE_TEXT="просмотр текста ";
    static final String PAGE_SCREEN_TEXT_URL="ссылка ";
    static final String PAGE_SCREEN_TEXT_TITLE="заголовок ";
    static final String PAGE_SCREEN_TEXT_STATE="состояние ";
    static final String PAGE_SCREEN_TEXT__PROGRESS="процент загрузки ";
    static final String PAGE_SCREEN_TEXT_SIZE="размер ";
    static final String PAGE_SCREEN_PROMPT_MESSAGE="Запрос на ввод текста от вебстраницы";
    static final String PAGE_SCREEN_ALERT_MESSAGE="Сообщение от вебстраницы ";
    static final String PAGE_SCREEN_CONFIRM_MESSAGE="Запрос подтверждения от вебстраницы ";
    static final String PAGE_ANY_PROMPT_TEXT_FILTER="Введите строку для поиска текста";
    static final String PAGE_ANY_PROMPT_ADDRESS="Введите новый интернет адрес";
    static final String PAGE_ANY_PROMPT_NEW_TEXT="Введите новое значение для элемента";
    static final String PAGE_SCREEN_ANY_FIRST_ELEMENT="Начало списка элементов";
    static final String PAGE_SCREEN_ANY_END_ELEMENT="Конец списка элементов";
    static final String PAGE_SCREEN_ANY_HAVENO_ELEMENT="Элементы не найдены";
    static final String PAGE_ANY_PROMPT_ACCEPT_DOWNLOAD="Запрос на загрузку файла";
    static final String PAGE_DOWNLOAD_START="Загрузка файла начата";
    static final String PAGE_DOWNLOAD_FINISHED="Загрузка файла завершена";
    static final String PAGE_DOWNLOAD_FAILED="Загрузка файла прервана";
    static final String PAGE_DOWNLOAD_FIELD_FILESIZE="Размер файла ";
    static final String PAGE_DOWNLOAD_FIELD_FILETYPE="Тип ";
    static final String PAGE_DOWNLOAD_FIELD_PROGRESS="Состояние ";
    static final String PAGE_DOWNLOAD_FIELD_PROGRESS_FINISHED="загружено";
    static final String PAGE_ANY_PROMPT_TAGFILTER_NAME="Введите имя тега для поиска";
    static final String PAGE_ANY_PROMPT_TAGFILTER_VALUE="имя атрибута";
    static final String PAGE_ANY_PROMPT_TAGFILTER_ATTR="значение атрибута";
    static final String POPUP_TITLE_NEW_PAGE="Открытие новой страницы";
    static final String POPUP_TITLE_CHANGE_ELEMENT_EDIT="Изменение элемента на вебстранице";
    static final String POPUP_TITLE_CHANGE_TEXT_FILTER="Поиск элемента по тексту";
    static final String POPUP_TITLE_CHANGE_TAG_FILTER="Поиск элемента по имени тега и его атруибуту";
    static final String POPUP_TITLE_WEB_MESSAGE="Сообщение от вебсценария";

    // downloader settings
    static final String DEFAULT_DOWNLOAD_DIR=".";
    static final int BUFFER_SIZE=1024*1024;

    // FIXME: get current screen text table width from environment and do it any time but not from constant
    static final int TEXT_SCREEN_WIDTH=100;

    static String[] splitTextForScreen(String string)
    {
    	Vector<String> text=new Vector<String>();
	if(string==null||string.isEmpty()) 
return text.toArray(new String[(text.size())]);
	int i=0;
	while(i<string.length())
	{
	    String line;
	    if(i+TEXT_SCREEN_WIDTH>=string.length())
	    { // last part of string fit to the screen
		line=string.substring(i);
	    } else
	    { // too long part
		line=string.substring(i,i+TEXT_SCREEN_WIDTH-1);
		// check for new line char
		int nl=line.indexOf('\n');
		if(nl!=-1)
		{ // have new line char, cut line to it
		    line=line.substring(0,nl);
		    i++; // skip new line
		} else
		{ // walk to first stopword char at end of line
		    int sw=line.lastIndexOf(' ');
		    if(sw!=-1)
		    { // have stop char, cut line to it (but include)
		    	line=line.substring(0,sw);
		    	i++;
		    }
		}
	    }
	    text.add(line);
	    i+=line.length();
	}
	return text.toArray(new String[(text.size())]);
    }

    static public <T> T defaultIfNull(T value,T ifnull)
    {
    	if(value!=null) return value;else return ifnull;
    }



}
