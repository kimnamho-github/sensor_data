package com.example.distance;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.LinkedList;

public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener { // MainActivity 클래스가 상속 받으면서 구현
    private SensorManager sm;
    private Sensor accelSensor;
    private Sensor gyroSensor;
    private SensorEventListener accelListener;
    private SensorEventListener gyroListener;
    private EditText edit_position;
    private EditText edit_motion;
    private EditText edit_sequence;
    private Spinner spinner_delay;
    private Button btn_reset;
    private Button btn_start;
    private final String[] items = {"DELAY_FASTEST", "DELAY_GAME", "DELAY_NORMAL", "DELAY_UI"};
    private int delay = SensorManager.SENSOR_DELAY_NORMAL;
    private boolean btn_set = true;
    private LinkedList<Storage> list;
    private LinkedList<Store> accelList;
    private LinkedList<Store> gyroList;
    private Long starttime;
    private Long endtime;


    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) { // savedInstanceState 으로 변경된 값 유지
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // activity_main.xml 레이아웃 출력

        // findViewById를 이용하여 버튼 및 글자를 변경
        edit_position = (EditText) findViewById(R.id.editPosition);
        edit_motion = (EditText) findViewById(R.id.editMotion);
        edit_sequence = (EditText) findViewById(R.id.editSequence);
        spinner_delay = (Spinner) findViewById(R.id.spinnerDelay);
        btn_reset = (Button) findViewById(R.id.btnReset);
        btn_start = (Button) findViewById(R.id.btnStart);

        accelListener = new AccelListener(); // 가속도 센서 객체 생성
        gyroListener = new GyroListener(); // 자이로 센서 객체 생성

        list = new LinkedList<Storage>();
        accelList = new LinkedList<Store>();
        gyroList = new LinkedList<Store>();

        sm = (SensorManager) getSystemService(SENSOR_SERVICE); // SensorManager 인스턴스를 가져옴
        accelSensor = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION); // 가속도 센서 인스턴스를 가져옴
        gyroSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE); // 자이로 센서 인스턴스를 가져옴


        ArrayAdapter<String> adapter_delay = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
        spinner_delay.setAdapter(adapter_delay);
        spinner_delay.setSelection(2);
        spinner_delay.setOnItemSelectedListener(this);

        btn_reset.setOnClickListener(new View.OnClickListener() { // reset 버튼 클릭시 설정
            @Override
            public void onClick(View v) {
                edit_position.setText(""); // 위치
                edit_motion.setText(""); // 움직임
                edit_sequence.setText(""); // 순서
                spinner_delay.setSelection(2);
                delay = SensorManager.SENSOR_DELAY_NORMAL; // 주기
                Toast.makeText(MainActivity.this, "설정 초기화", Toast.LENGTH_SHORT).show();
            }
        });

        btn_start.setOnClickListener(new View.OnClickListener() { // start 버튼 클릭 시 측정
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                if (btn_set) {
                    sm.unregisterListener(accelListener); // 가속도 센서 해제
                    sm.unregisterListener(gyroListener); // 자이로 센서 해제

                    resetList();
                    btn_reset.setEnabled(false); // 객체 비활성화

                    sm.registerListener(accelListener, accelSensor, delay); // 가속도 센서 감지
                    sm.registerListener(gyroListener, gyroSensor, delay); // 자이로 센서 감지
                    starttime = System.currentTimeMillis(); // 현재 시간을 밀리세컨드 단위로 반환

                    Toast.makeText(MainActivity.this, "센서 측정 시작", Toast.LENGTH_SHORT).show();

                    btn_set = false;
                    btn_start.setText("STOP!");
                } else {
                    sm.unregisterListener(accelListener); // 가속도 센서 해제
                    sm.unregisterListener(gyroListener); // 자이로 센서 해제
                    endtime = System.currentTimeMillis(); // 현재 시간을 밀리세컨드 단위로 반환
                    btn_start.setEnabled(false); // 객체 비활성화

                    mergeList();
                    saveFile();

                    btn_reset.setEnabled(true); // 객체 활성화
                    btn_start.setEnabled(true); // 객체 활성화

                    btn_set = true;
                    btn_start.setText("START!");

                }
            }
        });
    }

    private void mergeList() { // list merge 함수
        Store accelTemp;
        Store gyroTemp;
        Iterator accitr = accelList.iterator(); // 가속도 센서 iterator 생성
        Iterator gyroitr = gyroList.iterator(); // 자이로 센서 iterator 생성

        while (accitr.hasNext() && gyroitr.hasNext()) { // 읽어올 요소가 있는지 확인 있으면 true, 없으면 false
            accelTemp = (Store) accitr.next(); // 가속도 센서 데이터를 반환
            gyroTemp = (Store) gyroitr.next(); // 자이로 센서 데이터를 반환
            list.add(new Storage(accelTemp, gyroTemp)); // list 에 가속도 센서와 자이로 센서의 데이터 값을 추가

        }
    }

    private void resetList() { // list reset 함수
        list.clear(); // list 에 있는 저장된 데이터의 모든 값 삭제
        accelList.clear(); // 가속도 센서 데이터 모두 삭제
        gyroList.clear(); // 자이로 센서 데이터 모두 삭제
    }

    private void saveFile() { // 파일 저장 함수
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); // 외부 저장소 DOWNLOADS 에 저장
        String filepath = "sensor_data"; // wearable 폴더에 파일 저장
        File file = new File(path, filepath);

        Storage storeTemp;
        Iterator itr = list.iterator(); // list iterator 생성
        String title = "accel_x,accel_y,accel_z,accel_ts,gyro_x,gyro_y,gyro_z,gyro_ts\n";
        String time;
        String info = "";

        file.mkdirs(); // 디렉토리 생성

        String tempfile = edit_motion.getText().toString() + "_" + edit_position.getText().toString()
                + "_" + edit_sequence.getText().toString() + ".csv"; // csv 파일 형식으로 저장

        String filename = file.getPath().toString() + "/" + tempfile;

        Toast.makeText(this, filename + " 파일 저장 중", Toast.LENGTH_SHORT).show();

        try {
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(title.getBytes());

            while (itr.hasNext()) {
                storeTemp = (Storage) itr.next();
                fos.write(storeTemp.toString().getBytes());
            }
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "파일 저장 완료", Toast.LENGTH_SHORT).show();

    }


    protected void onResume() { // 어플 재접속시 어플 재실행
        super.onResume();
        sm.unregisterListener(accelListener); // 가속도 센서 해제
        sm.unregisterListener(gyroListener); // 자이로 센서 해제

        Toast.makeText(MainActivity.this, "재실행 요청", Toast.LENGTH_SHORT).show();
    }

    protected void onPause() { // 어플 나가면 측정 정지
        super.onPause();
        sm.unregisterListener(accelListener); // 가속도 센서 해제
        sm.unregisterListener(gyroListener); // 자이로 센서 해제

        Toast.makeText(MainActivity.this, "센서 측정 정지", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStop() { // stop 버튼 클릭시 측정 정지
        super.onStop();
        sm.unregisterListener(accelListener); // 가속도 센서 해제
        sm.unregisterListener(gyroListener); // 자이로 센서 해제

        Toast.makeText(MainActivity.this, "센서 측정 정지", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() { // 어플 종료
        super.onDestroy();
        sm.unregisterListener(accelListener); // 가속도 센서 해제
        sm.unregisterListener(gyroListener); // 자이로 센서 해제

        Toast.makeText(MainActivity.this, "센서 측정 프로그램 종료", Toast.LENGTH_SHORT).show();
    }

    @Override // 센서 딜레이 선택
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0:
                delay = SensorManager.SENSOR_DELAY_FASTEST; // 가능한 가장 자주 센서값을 가져옴
                break;
            case 1:
                delay = SensorManager.SENSOR_DELAY_GAME; // 게임에 적합한 정도로 센서값을 가져옴
                break;
            case 2:
                delay = SensorManager.SENSOR_DELAY_NORMAL; // 화면 방향이 전환될 때 적합한 정도로 센서값을 가져옴
                break;
            case 3:
                delay = SensorManager.SENSOR_DELAY_UI; // 사용자 인터페이스를 표시하기에 적합한 정도로 센서값을 가져옴
                break;
            default:
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private class AccelListener implements SensorEventListener {
        @Override // 가속도 센서 값이 변경되면 호출
        public void onSensorChanged(SensorEvent event) {
            accelList.add(new Store(event.values[0], event.values[1], event.values[2], event.timestamp));
        }

        @Override // 가속도 센서 정밀도가 변경되면 호출
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    private class GyroListener implements SensorEventListener {
        @Override // 자이로 센서 값이 변경되면 호출
        public void onSensorChanged(SensorEvent event) {
            gyroList.add(new Store(event.values[0], event.values[1], event.values[2], event.timestamp));
        }

        @Override // 자이로 센서 정밀도가 변경되면 호출
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}
