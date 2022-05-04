package com.example.distance;

public class Storage {
    private Store accel;
    private Store gyro;

    Storage(Store accel, Store gyro) { // 가속도 센서, 자이로 센서 매개변수 생성자
        this.accel = accel; // 가속도 센서 값
        this.gyro = gyro; // 자이로 센서 값

    }

    @Override
    public String toString() { // 결과 값을 String 형 으로 반환
        return accel.toString() + "," + gyro.toString() + "\n"; // 가속도, 자이로 센서의 데이터 값 반환
    }
}

class Store {
    public String x;
    public String y;
    public String z;
    public String ts;

    Store(float x, float y, float z, long ts) {
        this.x = String.valueOf(x); // x축의 값
        this.y = String.valueOf(y); // y축의 값
        this.z = String.valueOf(z); // z축의 값
        this.ts = String.valueOf(ts); // 타임 스탬프
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z + "," + ts; // x, y, z 축의 가속도 센서 값과, 자이로 센서 값, 타임스탬프 반환

    }
}
