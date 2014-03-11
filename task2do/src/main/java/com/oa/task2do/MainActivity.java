package com.oa.task2do;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends FragmentActivity implements DialogListener   {

    Singleton singleton=null;
    private static final int EDIT_TASK = 1232;
    private static final int RESULT_MAP = 1233;
    private static final int RESULT_SPEECH = 1234;
    private TaskListBaseAdapter currentList;
    private TextWatcher tw;
    private int taskIdSelected= -1;
    private int hasAlarm=0; //0 no , 1 yes
    private int isDone=0;   //0 no , 1 yes
    private double mapLongitude= -1;
    private double mapLatitude= -1;
    private int mapRadius= 500;
    private int timeHour = -1;
    private int timeMinute= -1;
    private int dateYear= -1;
    private int dateMonth= -1;
    private int dateDay= -1;
    //back key press event
    private long lastPressedTime;
    private static final int PERIOD = 2000;
    private boolean extras = true;
    private int ifEditTask = -1;
    //location alert
    private LocationManager lm=null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* avoid keyboard show automatic */
        ListView listView = (ListView) findViewById(R.id.listView);

        //close keyboard and set focusable false to etNetTask
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(listView.getWindowToken(), 0);

        /* If there is Tasks in the DataBase restore them */
        if (singleton.getInstance(this).getArrayList().isEmpty())
            restoreFromDb();

        currentList = new TaskListBaseAdapter(this, singleton.getInstance(this).getArrayList());

        /* when etNewTask pressed then show the extra buttons */
        EditText newTask = (EditText) findViewById(R.id.etNewTask);
        newTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).isActive()) {
                    LinearLayout linearLayout = (LinearLayout) findViewById(R.id.extraOptions);
                    if (linearLayout.getVisibility()== View.VISIBLE){
                        linearLayout.setVisibility(LinearLayout.GONE);
                    }
                    else if (currentList.get_info_ifEditText()==-1){
                        linearLayout.setVisibility(LinearLayout.VISIBLE);
                        //to change the variables if i canceled selected item
                        initialize_variables();
                    }
                    else if (currentList.get_info_ifEditText()==-1) linearLayout.setVisibility(LinearLayout.GONE);
                }
            }
        });


        /* inflate extra bar when text change */
        tw = new TextWatcher() {
            LinearLayout linearLayout = (LinearLayout) findViewById(R.id.extraOptions);
            public void afterTextChanged(Editable s){
                if (s.length()>0 && currentList.get_info_ifEditText()==-1)
                    linearLayout.setVisibility(LinearLayout.VISIBLE);
                else linearLayout.setVisibility(LinearLayout.GONE);
            }
            public void  beforeTextChanged(CharSequence s, int start, int count, int after){
                if (count<0)
                    linearLayout.setVisibility(LinearLayout.GONE);
            }
            public void  onTextChanged (CharSequence s, int start, int before,int count) {
                if (count<0)
                    linearLayout.setVisibility(LinearLayout.GONE);
            }
        };
        EditText et = (EditText) findViewById(R.id.etNewTask);
        et.addTextChangedListener(tw);

        //swipe listener
        SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener(listView,
                new SwipeDismissListViewTouchListener.OnDismissCallback() {
                                 public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                         for (int position : reverseSortedPositions) {
                                             //currentList.remove(currentList.getItem(position));
                                             int id=((Task)currentList.getItem(position)).getID();
                                             currentList.remove(position);
                                             Task t = currentList.getItemByID(id);
                                             if (t.get_done()==1){
                                                 DeleteFromDb(t, position);
                                             }
                                             else {
                                                 t.set_alarm(0);
                                                 t.set_done(1);
                                             }

                                             updateTaskInDb(t);

                                             //updateTaskInArray(t,this.onDismiss());
                                             updateListView();
                                            }
                                     currentList.notifyDataSetChanged();
                                     }
                             });
         listView.setOnTouchListener(touchListener);
         listView.setOnScrollListener(touchListener.makeScrollListener());

    }

    @Override
    protected void onResume(){
        super.onResume();
        /* If there is Tasks in the DataBase restore them */
        singleton.getInstance(this).clearArrayList();
        if (singleton.getInstance(this).getArrayList().isEmpty())
            restoreFromDb();
        updateListView();
        currentList.notifyDataSetChanged();
    }

    /* handel with back key
     * when pressed twice the application closed
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            switch (event.getAction()) {
                case KeyEvent.ACTION_DOWN:
                    if (event.getDownTime() - lastPressedTime < PERIOD) {
                        finish();
                    } else {
                        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.extraOptions);
                        linearLayout.setVisibility(LinearLayout.GONE);
                        if (currentList.get_info_ifEditText()==-1) ifEditTask=-1;
                        updateListView();
                        Toast.makeText(getApplicationContext(), "Press again to exit.",
                                Toast.LENGTH_SHORT).show();
                        lastPressedTime = event.getEventTime();
                    }
                    return true;
            }
        }
        return false;
    }


    /**
     * All types of Dialogs
     */
    /* Time-Picker Dialog */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void showTimePickerDialog(View v) {
        DialogFragment newFragment = new TimePickerFragment();

        // add details if user want to edit exist task
        if (timeHour != 0 && timeMinute != 0){
            Bundle dataBundle = new Bundle();
            dataBundle.putInt("timeHour", timeHour);
            dataBundle.putInt("timeMinute", timeMinute);
            newFragment.setArguments(dataBundle);
        }
        newFragment.show(getFragmentManager(), "timePicker");
    }

    //show alert dialog
    public void alertDialog(View v) {
        DialogFragment newFragment = new com.oa.task2do.AlertDialog();
        // add details if user want to edit exist task
        newFragment.show(getFragmentManager(), "alertPicker");
    }

    /* Date-Picker Dialog */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();

        // add details if user want to edit exist task
        if (dateYear != 0 && dateMonth != 0 && dateDay != 0){
            Bundle dataBundle = new Bundle();
            dataBundle.putInt("dateYear", dateYear);
            dataBundle.putInt("dateMonth", dateMonth);
            dataBundle.putInt("dateDay", dateDay);
            newFragment.setArguments(dataBundle);
            System.out.println("save bundle----------" + dateDay + "." + dateMonth + "." + dateYear);
        }
        newFragment.show(getFragmentManager(), "datePicker");
    }
    /* Location Activity */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void showLocationDialog(View v) {
        Intent intent = new Intent(this, LocationActivity.class);

        // add details if user want to edit task
        if (mapLatitude != 0 && mapLongitude != 0){
            intent.putExtra("mapLatitude", mapLatitude);
            intent.putExtra("mapLongitude", mapLongitude);
            intent.putExtra("radius", mapRadius);
        }

        try {
            startActivityForResult(intent, RESULT_MAP);
        } catch (ActivityNotFoundException a) {
            Toast t = Toast.makeText(getApplicationContext(),
                    "Opps! Your device doesn't support Google-Maps",
                    Toast.LENGTH_SHORT);
            t.show();
        }
    }
    /* voice Dialog */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void showVoiceDialog(View v) {
        /* Fire an intent to start the voice recognition activity. */
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Voice recognition...");
        try {
            startActivityForResult(intent, RESULT_SPEECH);
            EditText et = (EditText) findViewById(R.id.etNewTask);
            et.setText("");
        } catch (ActivityNotFoundException a) {
            Toast t = Toast.makeText(getApplicationContext(),
                    "Opps! Your device doesn't support Speech to Text",
                    Toast.LENGTH_SHORT);
            t.show();
        }
    }

    /**
     *
     * Returning data from FragmentDialogs
     * (Time dialog)
     * (Date dialog)
     */
    @Override
    public void onFinishEditDialog(Intent data) {
        /* For TimePicker Fragment */
        if (data.getExtras().containsKey("hour"))
            timeHour = data.getExtras().getInt("hour");
        if (data.getExtras().containsKey("minute"))
            timeMinute = data.getExtras().getInt("minute");

        /* For DatePicker Fragment */
        if (data.getExtras().containsKey("year"))
            dateYear = data.getExtras().getInt("year");
        if (data.getExtras().containsKey("month"))
            dateMonth = data.getExtras().getInt("month");
        if (data.getExtras().containsKey("day"))
            dateDay = data.getExtras().getInt("day");

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case EDIT_TASK: {
                if (resultCode == EDIT_TASK) {
                    // Receive String from Speach recognition
                    ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    EditText editText_newTask = (EditText) findViewById(R.id.etNewTask);
                    editText_newTask.setText(text.get(0));
                }
                break;
            }
            case RESULT_MAP: {
                if (resultCode == RESULT_OK ) {
                    // Receive 2 parameters from MAP activity
                    mapLongitude= data.getDoubleExtra("longitude", 0);
                    mapLatitude= data.getDoubleExtra("latitude", 0);
                    mapRadius= data.getIntExtra("radius", 0);
                }
                break;
            }
            case RESULT_SPEECH: {
                if (resultCode == RESULT_OK && data != null) {
                    // Receive String from Speach recognition
                    ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    EditText editText_newTask = (EditText) findViewById(R.id.etNewTask);
                    editText_newTask.setText(text.get(0));
                }
                break;
            }

        }
    }

    public void restoreFromDb(){
        List<Task> list = singleton.getInstance(this).getDb().getAllTasks();
        for(Task task : list){
            //int isDone= task._done;
            //String str = new String( task.getTaskMessage() );
            singleton.getInstance(this).getArrayList().add(0,task);
            //System.out.println();
        }
        updateListView();
    }

    public void updateListView(){
        ListView lv = (ListView) findViewById(R.id.listView);
        TaskListBaseAdapter currentList = new TaskListBaseAdapter(this, singleton.getInstance(this).getArrayList());
        lv.setAdapter(currentList);
    }

    /* place all the details from the task in upper EditText and local variables that will send by intent to dialogs */
    public void editTask (View view) {

        /* close the upper bar */
        LinearLayout linearLayout1 = (LinearLayout) findViewById(R.id.extraOptions);
        if (linearLayout1.getVisibility()== View.VISIBLE )
            linearLayout1.setVisibility(LinearLayout.GONE);

        /* Get the specific Task in this place */
        ListView listView = (ListView) findViewById(R.id.listView);
        int position = listView.getPositionForView(view);
        Task selectedTask = (Task) listView.getItemAtPosition(position);

        currentList.info_IfEditTask(selectedTask.getID());

        //change list view height for the inflate
        ListView list = (ListView) findViewById(R.id.listView);
        list.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,getWallpaperDesiredMinimumHeight() ) );

        // Location
        if (selectedTask._mapLongitude != -1 )
            mapLongitude = selectedTask._mapLongitude;
        if (selectedTask._mapLatitude != -1 )
            mapLatitude = selectedTask._mapLatitude;
        // Date
        if (selectedTask._dateYear != -1 )
            dateYear = selectedTask._dateYear;
        if (selectedTask._dateMonth != -1 )
            dateMonth = selectedTask._dateMonth;
        if (selectedTask._dateDay != -1 )
            dateDay = selectedTask._dateDay;
        // Time
        if (selectedTask._timeHour != -1 )
            timeHour = selectedTask._timeHour;
        if (selectedTask._timeMinute != -1 )
            timeMinute = selectedTask._timeMinute;

        //update task text
//        EditText editText = (EditText) findViewById(R.id.editText);
//        editText.setText(selectedTask.getTaskMessage(), TextView.BufferType.EDITABLE);

        ifEditTask=taskIdSelected = selectedTask.getID();
        //System.out.println(timeHour+":"+timeMinute+" "+dateDay+"/"+dateMonth+"/"+dateYear+" ("+mapLongitude+","+mapLatitude+") -- "+message);
        updateListView();
        currentList.notifyDataSetChanged();

        //showEditAlert("EDIT", selectedTask, view);

        /* close keyboard and set focusable false to etNetTask */
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(listView.getWindowToken(), 0);
    }

    public void editTaskText (View view){
        // Message
        EditText message = (EditText) findViewById(R.id.etNewTask);
        message.setText(currentList.getItemByID(taskIdSelected).getTaskMessage() );
    }

    // if user check the dateButton
    private void createAlarmAtDate(Task task){

        Intent intent = new Intent();
        intent.setAction("com.oa.task2do.ReminderBroadCastReceiver");
        intent.putExtra("taskMessage", task._taskMessage );
        intent.putExtra("taskId", task._id);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, task.getID(), intent, 0);

        /* getting all parameters of create Date ob from task */
        Date date = new Date();
        // fix date before and set a default day
        date.setYear(date.getYear()+1900);
        date.setMonth(date.getMonth()+1);
        date.setDate(date.getDay());
        date.setHours(10);
        date.setMinutes(0);
        //System.out.println("----------------"+task._dateYear+" "+task._dateMonth+" "+task._dateDay+" "+task._timeHour+" "+task._timeMinute);
        if (task._dateDay != -1 && task._dateMonth != -1 && task._dateYear != -1){
            date.setYear(   task._dateYear  );
            date.setMonth(  task._dateMonth );
            date.setDate(   task._dateDay   );
        }
        if (task._timeHour != -1 && task._timeMinute != -1){
            date.setHours(  task._timeHour  );
            date.setMinutes(task._timeMinute);
        }
        if (task._timeHour == -1 && task._timeMinute == -1 && task._dateDay == -1 && task._dateMonth == -1 && task._dateYear == -1){
            return;
        }
        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + millisecondsUntilDate() , pendingIntent);
    }

    public long millisecondsUntilDate(){
        Calendar cal = Calendar.getInstance();
        if (timeHour != -1 && timeMinute != -1 && dateDay == -1 && dateMonth == -1 && dateYear == -1){
            cal.set(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH),timeHour,timeMinute);
        }

        if (dateDay != -1 && dateMonth != -1 && dateYear != -1 && timeHour != -1 && timeMinute != -1) {
            cal.set(dateYear, dateMonth, dateDay, timeHour, timeMinute);
        }

        if (dateDay != -1 && dateMonth != -1 && dateYear != -1 && timeHour == -1 && timeMinute == -1) {
            cal.set(dateYear,dateMonth,dateDay,10,0);
        }
        Calendar now = Calendar.getInstance();
        long diff_in_ms = cal.getTimeInMillis()-now.getTimeInMillis();
        return diff_in_ms;
    }

    public void done(View view) {

        ListView listView = (ListView) findViewById(R.id.listView);
        int position = listView.getPositionForView(view);
        Task selectedTask = (Task) listView.getItemAtPosition(position);
        Toast.makeText(MainActivity.this, "DONE : " + " " +
                selectedTask.getTaskMessage(), Toast.LENGTH_LONG).show();

        // maby do this in restore_from_db()
        /* change button background */
//        Button btDone = (Button) findViewById(R.id.doneButton);
//        btDone.setBackgroundResource(android.R.drawable.checkbox_on_background);
        ifEditTask = taskIdSelected = selectedTask.getID();
        selectedTask.set_done(1);
        updateTaskInDb(selectedTask);
        //updateTaskInArray(selectedTask);
        updateListView();   //check d
        //restoreFromDb();

        currentList.notifyDataSetChanged();

        initialize_variables();
    }
    public void delete(View view) {
        ListView listView = (ListView) findViewById(R.id.listView);
        int position = listView.getPositionForView(view);
        Task selectedTask = (Task) listView.getItemAtPosition(position);
        taskIdSelected=selectedTask.getID();
        showDeleteAlert("DELETE", selectedTask.getTaskMessage(), view);

    }

    public void DeleteFromDb(Task taskToDelete, int position){
        singleton.getInstance(this).getArrayList().remove(position);
        singleton.getInstance(this).getDb().deleteTask( taskToDelete);
        updateListView();
    }


    public void saveTask(View view){
        /* only if user put text for the task - we continue to save it */
        EditText description = (EditText) findViewById(R.id.etNewTask);
        if (!description.getText().toString().isEmpty() || ifEditTask != -1){

            if ( ifEditTask == -1 ){
            /* not in edit mode */
                /* Create ID for the Task by get currentTimeMillis of this moment */
                int nowUseAsId = (int) (long) System.currentTimeMillis();
                if (nowUseAsId<0) nowUseAsId*=-1;

                /* Text Message */
                String taskMessage = description.getText().toString();

                //System.out.println(timeHour+":"+timeMinute+" "+dateDay+"/"+dateMonth+"/"+dateYear+" ("+mapLongitude+","+mapLatitude+") -- "+taskMessage);
                Task task = new Task(nowUseAsId, taskMessage, dateYear, dateMonth, dateDay, timeHour, timeMinute, mapLongitude, mapLatitude,hasAlarm,isDone);
                System.out.println("********************************USUAL*********************************************************");
                System.out.println(timeHour+":"+timeMinute+" "+dateDay+"/"+dateMonth+"/"+dateYear+" ("+mapLongitude+","+mapLatitude+") -- "+taskMessage);

               taskIdSelected=nowUseAsId;


                //create alarm from DATE+Time+ID details
                // using alarmManager
                if ((timeHour != -1 && timeMinute != -1 ) || (dateDay != -1 && dateMonth != -1 && dateYear != -1) ) {
                    hasAlarm=1;
                    task.set_alarm(hasAlarm);
                    isDone=0;
                    task.set_done(isDone);
                    createAlarmAtDate(task);
                }
                if (mapLatitude != -1 && mapLongitude != -1){
                    hasAlarm=1;
                    task.set_alarm(hasAlarm);
                    isDone=0;
                    task.set_done(isDone);
                    lm=(LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    setAlaramLocation(task, mapLatitude,mapLongitude,mapRadius);
                }
                singleton.getInstance(this).getArrayList().add(0, task);

                //-----continue checking from here -> to register date & cancel alarmManager after if click done
                saveToDb(task);
                //updateTaskInArray(task);
            }
            else{
                /* is in edit mode */
                // try to update this task in DB
                String taskMessage;
                /* Text Message */
                if (description.getText().length()<1)
                     taskMessage = currentList.getItemByID(taskIdSelected).getTaskMessage();
                else taskMessage = description.getText().toString();
                Task editedTask = new Task(taskIdSelected, taskMessage, dateYear, dateMonth, dateDay, timeHour, timeMinute, mapLongitude, mapLatitude,hasAlarm,isDone);
                System.out.println("********************************EDITED*********************************************************");
                System.out.println(timeHour+":"+timeMinute+" "+dateDay+"/"+dateMonth+"/"+dateYear+" ("+mapLongitude+","+mapLatitude+") -- "+taskMessage);

                if ((timeHour != -1 && timeMinute != -1 ) || (dateDay != -1 && dateMonth != -1 && dateYear != -1) ) {
                    hasAlarm=1;
                    editedTask.set_alarm(hasAlarm);
                    isDone=0;
                    editedTask.set_done(isDone);
                    createAlarmAtDate(editedTask);
                }
                if (mapLatitude != -1 && mapLongitude != -1){
                    hasAlarm=1;
                    editedTask.set_alarm(hasAlarm);
                    isDone=0;
                    editedTask.set_done(isDone);
                    lm=(LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    setAlaramLocation(editedTask, mapLatitude,mapLongitude,mapRadius);
                }
                updateTaskInDb(editedTask);
                updateTaskInArray(editedTask,view);

            }
            currentList.info_IfEditTask(-1);
            // initialize Task Message
            description.setText("");
            initialize_variables();
            //currentList.notifyDataSetChanged();
            updateListView();
        }
    }

    public void initialize_variables(){
        /* initialize variables */
        // Map
        mapLongitude= -1;
        mapLatitude= -1;
        // Time
        timeHour= -1;
        timeMinute= -1;
        // Date
        dateYear= -1;
        dateMonth= -1;
        dateDay= -1;

        hasAlarm=0;
        isDone=0;

        ifEditTask=-1;
        taskIdSelected=-1;
    }

    public void saveToDb(Task newTask){
        singleton.getInstance(this).getDb().addTask(newTask);
    }
    public void updateTaskInDb(Task editTask){
        singleton.getInstance(this).getDb().updateTask(editTask);
    }

    public void updateTaskInArray(Task editTask,View convertView){
        for (int i=0 ; i< currentList.getCount();i++)
        {
            if (((Task)currentList.getItem(i)).getID() == taskIdSelected)
            {
                ((Task)currentList.getItem(i)).setTaskMessage(editTask.getTaskMessage());
                ((Task)currentList.getItem(i)).set_timeHour(editTask.get_timeHour());
                ((Task)currentList.getItem(i)).set_timeMinute(editTask.get_timeMinute());
                ((Task)currentList.getItem(i)).set_dateYear(editTask.get_dateYear());
                ((Task)currentList.getItem(i)).set_dateMonth(editTask.get_dateMonth());
                ((Task)currentList.getItem(i)).set_dateDay(editTask.get_dateDay());
                ((Task)currentList.getItem(i)).set_mapLatitude(editTask.get_mapLatitude());
                ((Task)currentList.getItem(i)).set_mapLongitude(editTask.get_mapLongitude());
                ((Task)currentList.getItem(i)).set_alarm(editTask.get_alarm());
                ((Task)currentList.getItem(i)).set_done(editTask.get_done());

                if (editTask.get_alarm()==1){
                    System.out.println("############## ALARM: "+ editTask.get_alarm() +"#######################");

                }
                if (editTask.get_done()==1){
                    System.out.println("############## DONE: "+ editTask.get_done() +"#######################");

                }
            }
        }
    }

    //Set Alaram Location func
    public void setAlaramLocation(Task task, Double mapLatitude ,Double mapLongitude,double radius){
        System.out.println("*********************LOCATION***************************");
//                System.out.println("MainActivity:");
//                System.out.println("mapLatitude="+mapLatitude);
//                System.out.println("mapLongitude="+mapLongitude);
//                System.out.println("radius="+radius);
//                System.out.println("taskIdSelected="+taskIdSelected);
//                System.out.println("ifEditTask="+ifEditTask);
//                System.out.println("taskMessage="+currentList.getItemByID(taskIdSelected).getTaskMessage());

        // new intent
        Intent intent = new Intent();
        intent.setAction("com.oa.task2do.ReminderBroadCastReceiver");
        //intent.setAction("com.oa.task2do.LocationNotification");
        intent.putExtra("taskMessage", task.getTaskMessage() );
        intent.putExtra("taskId", task.getID());
        intent.putExtra("codeLocation", Integer.toString(task.getID()));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, task.getID(), intent, PendingIntent.FLAG_ONE_SHOT);

        lm.addProximityAlert(mapLatitude, mapLongitude, mapRadius, -1, pendingIntent);
    }
    //radio button in location layout
    public int onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        int radius=500;
        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radius500:
                if (checked){
                    RadioButton radio500 = (RadioButton) findViewById(R.id.radius500);
                    radius = Integer.parseInt((String) radio500.getText());
                    break;}
            case R.id.radius1000:
                if (checked){
                    RadioButton radio1000 = (RadioButton) findViewById(R.id.radius1000);
                    radius = Integer.parseInt((String) radio1000.getText());
                    break;}
            case R.id.radius1500:
                if (checked){
                    RadioButton radio1500 = (RadioButton) findViewById(R.id.radius1500);
                    radius = Integer.parseInt((String) radio1500.getText());
                    break;}
            case R.id.radius2000:
                if (checked){
                    RadioButton radio2000 = (RadioButton) findViewById(R.id.radius2000);
                    radius = Integer.parseInt((String) radio2000.getText());
                    break;}
        }
        return radius;
    }

    //display alert when task is deleted
    public void showDeleteAlert(String title, String message, final View view)
    {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set title
        alertDialogBuilder.setTitle(title);

        // set dialog message
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, close
                        // current activity
                        ListView listView = (ListView) findViewById(R.id.listView);
                        int position = listView.getPositionForView(view);
                        Task selectedTask = (Task) listView.getItemAtPosition(position);
//                        Toast.makeText(MainActivity.this, "deleted item : " + " " +
//                                selectedTask.getTaskMessage(), Toast.LENGTH_LONG).show();
                        DeleteFromDb(selectedTask, position);
                        currentList.notifyDataSetChanged();

                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing

                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.invite_friends:
                invite_friends();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void invite_friends(){
        String shareBody = "Check out Task 2Do,\ndownload it at https://play.google.com/store/apps/details?id=com.oa.task2do\n(by Ofir Aghai & Avishay Hajbi)";
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        if (sharingIntent != null) {
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Task 2Do");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.invite_friends_via)));
        } else {
            Toast.makeText(this, "You don't have any sharing app.", Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
