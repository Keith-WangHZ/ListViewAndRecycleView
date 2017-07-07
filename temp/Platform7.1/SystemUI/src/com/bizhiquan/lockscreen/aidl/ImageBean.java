package com.bizhiquan.lockscreen.aidl;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by alex on 6/6/16.
 */
public class ImageBean implements Parcelable {
    String img_id;                          
    String type_id;                         
    String type_name;                       
    String url_img;                         
    String url_local;                       
    String url_assets;                      
    String url_pv;                          
    String title;                           
    String content;                         
    String url_click;                       
    int img_type;
    String bgcolor;                         
    private String order;                   
    private String magazine_id;             
                                            
    private String daily_id;                
    private int is_collection;              
    private int always;                     

    public ImageBean() {
        super();
    }

    public ImageBean(Parcel parcel) {
        this.setImg_id(parcel.readString());
        this.setType_id(parcel.readString());
        this.setType_name(parcel.readString());
        this.setUrl_img(parcel.readString());
        this.setUrl_local(parcel.readString());
        this.setUrl_assets(parcel.readString());
        this.setUrl_pv(parcel.readString());
        this.setTitle(parcel.readString());
        this.setContent(parcel.readString());
        this.setUrl_click(parcel.readString());
        this.setImg_type(parcel.readInt());
        this.setBgcolor(parcel.readString());
        this.setOrder(parcel.readString());
        this.setMagazine_id(parcel.readString());
        this.setDaily_id(parcel.readString());
        this.setIs_collection(parcel.readInt());
        this.setAlways(parcel.readInt());
    }

    public static final Creator<ImageBean> CREATOR = new Creator<ImageBean>() {
        @Override
        public ImageBean createFromParcel(Parcel source) {
            return new ImageBean(source);
        }

        @Override
        public ImageBean[] newArray(int size) {
            return new ImageBean[size];
        }
    };

    // Parcelable implements methods
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(img_id);
        dest.writeString(type_id);
        dest.writeString(type_name);
        dest.writeString(url_img);
        dest.writeString(url_local);
        dest.writeString(url_assets);
        dest.writeString(url_pv);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(url_click);
        dest.writeInt(img_type);
        dest.writeString(bgcolor);
        dest.writeString(order);
        dest.writeString(magazine_id);
        dest.writeString(daily_id);
        dest.writeInt(is_collection);
        dest.writeInt(always);
    }



    public String getImg_id() {
        return img_id;
    }

    public void setImg_id(String img_id) {
        this.img_id = img_id;
    }

    public String getType_id() {
        return type_id;
    }

    public void setType_id(String type_id) {
        this.type_id = type_id;
    }

    public String getType_name() {
        return type_name;
    }

    public void setType_name(String type_name) {
        this.type_name = type_name;
    }

    public String getUrl_img() {
        return url_img;
    }

    public void setUrl_img(String url_img) {
        this.url_img = url_img;
    }

    public String getUrl_local() {
        return url_local;
    }

    public void setUrl_local(String url_local) {
        this.url_local = url_local;
    }

    public String getUrl_assets() {
        return url_assets;
    }

    public void setUrl_assets(String url_assets) {
        this.url_assets = url_assets;
    }

    public String getUrl_pv() {
        return url_pv;
    }

    public void setUrl_pv(String url_pv) {
        this.url_pv = url_pv;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUrl_click() {
        return url_click;
    }

    public void setUrl_click(String url_click) {
        this.url_click = url_click;
    }

    public int getImg_type() {
        return img_type;
    }

    public void setImg_type(int img_type) {
        this.img_type = img_type;
    }

    public String getBgcolor() {
        return bgcolor;
    }

    public void setBgcolor(String bgcolor) {
        this.bgcolor = bgcolor;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public String getMagazine_id() {
        return magazine_id;
    }

    public void setMagazine_id(String magazine_id) {
        this.magazine_id = magazine_id;
    }

    public String getDaily_id() {
        return daily_id;
    }

    public void setDaily_id(String daily_id) {
        this.daily_id = daily_id;
    }

    public int getIs_collection() {
        return is_collection;
    }

    public void setIs_collection(int is_collection) {
        this.is_collection = is_collection;
    }

    public int getAlways() {
        return always;
    }

    public void setAlways(int always) {
        this.always = always;
    }
}
