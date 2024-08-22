package com.payhere.mqtt

object MqttEvent {
    const val BASE_TOPIC_COMMON = "common" //외부연동결제
    const val BASE_TOPIC_PAYMENT = "payments" //외부연동결제
    const val BASE_TOPIC_SELLERS = "sellers" //관제, 제어
    const val BASE_TOPIC_PLATFORMS = "platforms" //관제, 제어

    const val CYCLE = "cycle"
    const val STATUS = "status"



    const val ALIVE = "alive"
    const val CONNECTION_CHECK = "connection_check"
    object Request {
        // 카드
        // 카드결제 승인 요청
        const val CAPTURE_CARD = "request_capture_card"
        // 카드결제 환불 요청
        const val REFUND_CARD = "request_refund_card"

        // 현금
        // 현금결제 승인 요청
        const val CAPTURE_CASH = "request_capture_cash"
        // 현금결제 환불 요청
        const val REFUND_CASH = "request_refund_cash"
        // 현금영수증 승인 요청
        const val CAPTURE_CASH_RECEIPT = "request_capture_cash_receipt"
        // 현금영수증 환불 요청
        const val REFUND_CASH_RECEIPT = "request_refund_cash_receipt"

        // 간편
        // 간편결제 승인 요청
        const val CAPTURE_QR = "request_capture_qr"
        // 간편결제 환불 요청
        const val REFUND_QR = "request_refund_qr"

        // ETC
        // 취소 요청
        const val CANCEL = "request_cancel"
        // 연결 요청
        const val CONNECT = "request_connect"
        // 연결해제 요청
        const val DISCONNECT = "request_disconnect"
        // 승인 요청
        const val CAPTURE = "request_capture"
        // 환불 요청
        const val REFUND = "request_refund"
        // 승인 취소 요청
        const val CAPTURE_CANCEL = "request_capture_cancel"
        // 환불 취소 요청
        const val REFUND_CANCEL = "request_refund_cancel"
        // 프린트
        const val PRINT = "request_print"
        // 서명 입력
        const val INPUT_SIGNATURE = "request_signature"
        // 전화번호 입력
        const val INPUT_PIN = "request_pin"
        // 카메라 입력
        const val INPUT_CAMERA = "request_camera"
        // 입력 취소
        const val INPUT_CANCEL = "request_input_cancel"
        // 현금통 오픈
        const val OPEN_CASH_DRAWER = "request_open_cash_drawer"
        // 키오스크 모드 활성
        const val KIOSK = "request_kiosk"

        const val SHUTDOWN = "request_shutdown"
        const val REBOOT = "request_reboot"
        const val LOGOUT = "request_logout"
        const val CLEAR_CACHE = "request_clear_cache"
        const val UPDATE = "request_update"
        const val REINSTALL = "request_reinstall"
    }
    object Result {
        // 카드
        // 카드결제 승인 결과
        const val CAPTURE_CARD = "result_capture_card"
        // 카드결제 환불 응답
        const val REFUND_CARD = "result_refund_card"

        // 현금
        // 현금결제 승인 결과
        const val CAPTURE_CASH = "result_capture_cash"
        // 현금결제 환불 응답
        const val REFUND_CASH = "result_refund_cash"
        // 현금영수증 승인 응답
        const val CAPTURE_CASH_RECEIPT = "result_capture_cash_receipt"
        // 현금영수증 환불 응답
        const val REFUND_CASH_RECEIPT = "result_refund_cash_receipt"

        // 간편
        // 간편결제 승인 응답
        const val CAPTURE_QR = "result_capture_qr"
        // 간편결제 환불 응답
        const val REFUND_QR = "result_refund_qr"

        // ETC
        // 취소 결과
        const val CANCEL = "result_cancel"
        // 연결 결과
        const val CONNECT = "result_connect"
        // 연결해제 결과
        const val DISCONNECT = "result_disconnect"
        // 승인 결과
        const val CAPTURE = "result_capture"
        // 환불 결과
        const val REFUND = "result_refund"
        // 승인 취소 결과
        const val CAPTURE_CANCEL = "result_capture_cancel"
        // 환불 취소 결과
        const val REFUND_CANCEL = "result_refund_cancel"
        // 프린트
        const val PRINT = "result_print"
        // 서명 입력
        const val INPUT_SIGNATURE = "result_signature"
        // 전화번호 입력
        const val INPUT_PIN = "result_pin"
        // 카메라 입력
        const val INPUT_CAMERA = "result_camera"
        // 입력 취소
        const val INPUT_CANCEL = "result_input_cancel"
        // 현금통 오픈
        const val OPEN_CASH_DRAWER = "result_open_cash_drawer"
        // 키오스크 모드 활성
        const val KIOSK = "result_kiosk"

        const val SHUTDOWN = "response_shutdown"
        const val REBOOT = "response_reboot"
        const val LOGOUT = "response_logout"
        const val CLEAR_CACHE = "response_clear_cache"
        const val UPDATE = "response_update"
        const val REINSTALL = "response_reinstall"
    }
}
