package com.example.demo.dto.wish;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class WishResponse {
    private Long wishNo;
    private String hotelName;
    private String totalAddress;

    public WishResponse(Long wishNo ,String hotelName, String totalAddress){
        this.wishNo=wishNo;
        this.hotelName=hotelName;
        this.totalAddress=totalAddress;
    }
}
