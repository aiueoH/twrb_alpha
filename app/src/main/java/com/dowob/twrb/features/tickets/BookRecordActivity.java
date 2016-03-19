package com.dowob.twrb.features.tickets;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dowob.twrb.R;
import com.dowob.twrb.database.BookRecord;
import com.dowob.twrb.database.BookableStation;
import com.dowob.twrb.features.shared.NetworkChecker;
import com.dowob.twrb.features.shared.SnackbarHelper;
import com.dowob.twrb.features.tickets.book.RandInputDialog;
import com.jakewharton.rxbinding.view.RxView;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class BookRecordActivity extends AppCompatActivity implements BookRecordModel.Observer {
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.textView_date)
    TextView date_textView;
    @Bind(R.id.textView_no)
    TextView no_textView;
    @Bind(R.id.textView_trainType)
    TextView trainType_textView;
    @Bind(R.id.textView_from)
    TextView from_textView;
    @Bind(R.id.textView_to)
    TextView to_textView;
    @Bind(R.id.textView_arrivalTime)
    TextView arrival_textView;
    @Bind(R.id.textView_departureTime)
    TextView departureTime_textView;
    @Bind(R.id.textView_fare)
    TextView fare_textView;
    @Bind(R.id.textView_totalPrice)
    TextView totalPrice_textView;
    @Bind(R.id.linearLayout_fare)
    LinearLayout fare_linearLayout;
    @Bind(R.id.linearLayout_totalPrice)
    LinearLayout totalPrice_linearLayout;
    @Bind(R.id.textView_qtu)
    TextView qtu_textView;
    @Bind(R.id.textView_personid)
    TextView personId_textView;
    @Bind(R.id.textView_code)
    TextView code_textView;
    @Bind(R.id.linearLayout_code)
    LinearLayout code_linearLayout;
    @Bind(R.id.button_cancel)
    Button cancel_button;
    @Bind(R.id.button_delete)
    Button delete_button;
    @Bind(R.id.button_book)
    Button book_button;
    @Bind(R.id.linearLayout_isBooked)
    LinearLayout isBooked_linearLayout;
    @Bind(R.id.linearLayout_isCancelled)
    LinearLayout isCancelled_linearLayout;
    @Bind(R.id.layout_trainType)
    LinearLayout trainType_layout;

    private View parentView;

    private BookRecord bookRecord;
    private ProgressDialog progressDialog;
    private BookRecordModel bookRecordModel;
    private boolean isWaitingRandomInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookrecord);
        ButterKnife.bind(this);
        parentView = trainType_layout;
        bookRecordModel = BookRecordModel.getInstance();
        bookRecordModel.registerObserver(this);
        EventBus.getDefault().register(this);
        getBookRecord();
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        bookRecordModel.unregisterObserver(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        EventBus.getDefault().post(new BookRecordAdapter.OnDisplayItemDetailEvent(-1));
    }

    private void getBookRecord() {
        Data data = EventBus.getDefault().getStickyEvent(Data.class);
        EventBus.getDefault().removeStickyEvent(data);
        bookRecord = data.getBookRecord();
    }

    private void updateUI() {
        setupToolbar();
        date_textView.setText(new SimpleDateFormat("yyyy/MM/dd E").format(bookRecord.getGetInDate()));
        String trainType = bookRecord.getTrainType();
        if (trainType != null && !trainType.isEmpty()) {
            trainType_layout.setVisibility(View.VISIBLE);
            trainType_textView.setText(trainType);
        } else
            trainType_layout.setVisibility(View.GONE);
        no_textView.setText(bookRecord.getTrainNo());
        from_textView.setText(BookableStation.getNameByNo(bookRecord.getFromStation()));
        to_textView.setText(BookableStation.getNameByNo(bookRecord.getToStation()));
        qtu_textView.setText(Integer.toString(bookRecord.getOrderQtu()));
        personId_textView.setText(bookRecord.getPersonId());
        code_textView.setText(bookRecord.getCode());
        code_linearLayout.setVisibility(bookRecord.getCode().isEmpty() ? View.GONE : View.VISIBLE);
        book_button.setVisibility(!bookRecord.getCode().isEmpty() || bookRecord.isCancelled() ? View.GONE : View.VISIBLE);
        cancel_button.setVisibility(bookRecord.getCode().isEmpty() || bookRecord.isCancelled() ? View.GONE : View.VISIBLE);
        isBooked_linearLayout.setVisibility(bookRecord.getCode().isEmpty() || bookRecord.isCancelled() ? View.GONE : View.VISIBLE);
        isCancelled_linearLayout.setVisibility(bookRecord.isCancelled() ? View.VISIBLE : View.GONE);
        setArrivalTime();
        setDepartureTime();
        setFareAndTotalPrice();
        RxView.clicks(delete_button)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribe(v -> {
                    BookRecordModel.getInstance().delete(bookRecord);
                    onBackPressed();
                });
        RxView.clicks(cancel_button)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribe(v -> {
                    onCancelButtonClick();
                });
        RxView.clicks(book_button)
                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribe(v -> {
                    onBookButtonClick();
                });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        setTitle(getString(R.string.toolbar_title_ticketDetail));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setArrivalTime() {
        if (bookRecord.getArrivalDateTime() != null) {
            String time = new SimpleDateFormat("HH:mm").format(bookRecord.getArrivalDateTime());
            arrival_textView.setVisibility(View.VISIBLE);
            arrival_textView.setText(time);
        } else {
            arrival_textView.setVisibility(View.GONE);
        }
    }

    private void setDepartureTime() {
        if (bookRecord.getDepartureDateTime() != null) {
            String time = new SimpleDateFormat("HH:mm").format(bookRecord.getDepartureDateTime());
            departureTime_textView.setVisibility(View.VISIBLE);
            departureTime_textView.setText(time);
        } else {
            departureTime_textView.setVisibility(View.GONE);
        }
    }

    private void setFareAndTotalPrice() {
        if (bookRecord.getFares() != 0) {
            fare_linearLayout.setVisibility(View.VISIBLE);
            fare_textView.setText(Integer.toString(bookRecord.getFares()));
            totalPrice_linearLayout.setVisibility(View.VISIBLE);
            totalPrice_textView.setText(Integer.toString(bookRecord.getFares() * bookRecord.getOrderQtu()));
        } else {
            fare_linearLayout.setVisibility(View.GONE);
            totalPrice_linearLayout.setVisibility(View.GONE);
        }
    }

    private void onBookButtonClick() {
        if (!NetworkChecker.isConnected(this)) {
            SnackbarHelper.show(parentView, getString(R.string.network_not_connected), Snackbar.LENGTH_LONG);
            return;
        }
        Observable.just(bookRecord.getId())
                .map(id -> bookRecordModel.book(this, id))
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(() -> progressDialog = ProgressDialog.show(this, "", getString(R.string.is_booking)))
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(captcha_stream -> {
                    progressDialog.dismiss();
                    if (captcha_stream == null) {
                        SnackbarHelper.show(parentView, "網路有些問題，再試一次看看", Snackbar.LENGTH_LONG);
                        return;
                    }
                    isWaitingRandomInput = true;
                    Bitmap captcha_bitmap = BitmapFactory.decodeByteArray(captcha_stream.toByteArray(), 0, captcha_stream.size());
                    RandInputDialog randInputDialog = new RandInputDialog(this, captcha_bitmap);
                    randInputDialog.show();
                });
    }

    public void onEvent(RandInputDialog.OnSubmitEvent e) {
        if (!isWaitingRandomInput)
            return;
        isWaitingRandomInput = false;
        final long bookRecordId = bookRecord.getId();
        Observable.just(e.getRandInput())
                .map(randInput -> bookRecordModel.sendRandomInput(bookRecordId, randInput))
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(() -> progressDialog = ProgressDialog.show(this, "", getString(R.string.is_booking)))
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    progressDialog.dismiss();
                    String s = BookManager.getResultMsg(this, result.getKey());
                    SnackbarHelper.show(parentView, s, Snackbar.LENGTH_LONG);
                });
    }

    private void onCancelButtonClick() {
        if (!NetworkChecker.isConnected(this)) {
            SnackbarHelper.show(parentView, getString(R.string.network_not_connected), Snackbar.LENGTH_LONG);
            return;
        }
        Observable.just(bookRecord.getId())
                .map(id -> bookRecordModel.cancel(id))
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(() -> progressDialog = ProgressDialog.show(this, "", "退票中"))
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    progressDialog.dismiss();
                    String s = getString(R.string.cancel_suc);
                    if (!result)
                        s = getString(R.string.cancel_fale);
                    SnackbarHelper.show(parentView, s, Snackbar.LENGTH_LONG);
                });
    }

    @Override
    public void notifyBookRecordCreate() {

    }

    @Override
    public void notifyBookRecordUpdate(long bookRecordId) {
        runOnUiThread(() -> {
            if (bookRecord.getId() == bookRecordId)
                updateUI();
        });
    }

    @Override
    public void notifyBookRecordRemove(long bookRecordId) {

    }

    public static class Data {
        private BookRecord bookRecord;

        public Data(BookRecord bookRecord) {
            this.bookRecord = bookRecord;
        }

        public BookRecord getBookRecord() {
            return bookRecord;
        }
    }
}