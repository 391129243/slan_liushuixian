#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <termios.h>

#include "Logger.h"
#include "ConfLocal.h"
#include "Uart.h"
#include "Common.h"
#include "MsgWrapper.h"

static UartCtx s_uart_ctx = {0};

/*******************************************
 *  波特率转换函数
********************************************/
speed_t ConvBaud(unsigned int baud_rate)
{
    switch(baud_rate){
        case 2400:
            return B2400;
        case 4800:
            return B4800;
        case 9600:
            return B9600;
        case 19200:
            return B19200;
        case 38400:
            return B38400;
        case 57600:
            return B57600;
        case 115200:
            return B115200;
        default:
            return B9600;
    }
}

int UartSetTermios(int fd, unsigned int speed, int data_bits, int stop_bits, char* parity_str)
{
    char parity;
    struct termios options;
    if (fd <= 0) {
        err_printf("Input err, dev fd is invalid.\n");
        return -1;
    }

    if (tcgetattr(fd, &options) != 0)
    {
        err_printf("Get uart dev attr failed, fd:%d.\n", fd);
        return -1;
    }

    if (parity_str) {
        parity = parity_str[0];
    } else {
        parity = 'N';
    }

    cfsetispeed(&options, ConvBaud(speed));        //填入串口输入端的波特率
    cfsetospeed(&options, ConvBaud(speed));        //填入串口输出端的波特率
    options.c_cflag |= CLOCAL;					   //控制模式，保证程序不会成为端口的占有者
    options.c_cflag |= CREAD;					   //控制模式，使能端口读取输入的数据
    options.c_cflag &= ~CRTSCTS;                   //不用任何流控制
    options.c_cflag &= ~CSIZE;                     //屏蔽其它数据位

    switch (data_bits)
    {
        case 5:
            options.c_cflag |= CS5;
            break;
        case 6:
            options.c_cflag |= CS6;
            break;
        case 7:
            options.c_cflag |= CS7;
           break;
        default:
            options.c_cflag |= CS8;
            break;
    }
    // 设置校验位
    switch (parity)
    {
        case 'o':
        case 'O': //设置为奇校验
            options.c_cflag |= PARENB;
            options.c_cflag |= PARODD;
            options.c_iflag |= INPCK;
            break;
        case 'e':
        case 'E': //设置为偶校验
            options.c_cflag |= PARENB;
            options.c_cflag &= ~PARODD;
            options.c_iflag |= INPCK;
           break;
        case 'n':
        case 'N': //无奇偶校验位
        default:
            options.c_cflag &= ~PARENB; //清除校验使能
            options.c_iflag &= ~INPCK;  //不使能奇偶校验检测
            break;
	}
    //设置停止位
    switch (stop_bits)
    {
        case 1:
            options.c_cflag &= ~CSTOPB; break;
        case 2:
            options.c_cflag |=  CSTOPB; break;
        default:
            options.c_cflag &= ~CSTOPB; break;
    }

    options.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG); // 非规范模式，禁用回显和中断字符处理  
    options.c_iflag &= ~(IXON | IXOFF | IXANY); // 禁用软件流控制  
    options.c_iflag &= ~(ICRNL | INLCR | ISTRIP);  
    options.c_oflag &= ~OPOST;          //输出模式，原始数据输出
    options.c_oflag &= ~(ONLCR | OCRNL); 
    options.c_cc[VMIN]  = 3;            //控制字符, 所要读取字符的最小数量
    options.c_cc[VTIME] = 1;            //控制字符, 读取第一个字符的等待时间    unit: (1/10)second
    
    tcflush(fd,TCIFLUSH);
    tcflush(fd, TCOFLUSH);
	if (tcsetattr(fd, TCSANOW, &options) != 0)
	{
        err_printf("Set uart dev attr failed, fd:%d.\n", fd);
		return -1;
	}

	return 0;    
}

int UartOpen(ConfUart *uart_cfg)
{
	int fd = -1;
    int flags = 0;
    int ret = 0;

    if (uart_cfg == NULL) {
        err_printf("Input err, ConfUart ptr is NULL.\n");
        return -1;
    }

    if (access(uart_cfg->dev_name, R_OK) != 0) {
        err_printf("Device:%s is not exits.\n", uart_cfg->dev_name);
        return -1;
    }

	fd = open(uart_cfg->dev_name , O_RDWR | O_NOCTTY | O_NDELAY);
	if (fd < 0)
	{
		err_printf("Uart open error!\n");
		return -1;
	}
    
    flags = fcntl(fd,F_GETFL,0);
    flags |= O_NONBLOCK;
	if(fcntl(fd, F_SETFL, flags) < 0)
	{   //阻塞，即使前面在open串口设备时设置的是非阻塞的，这里设为阻塞后，以此为准
		err_printf("Uart fcntl set nonblock failed\n");
        close(fd);
        return -1;
	}

    ret = UartSetTermios(fd, uart_cfg->baud_rate, uart_cfg->data_bits, uart_cfg->stop_bits, uart_cfg->parity);
    if (ret == -1) {
        err_printf("Uart set attr err.\n");
        close(fd);
        return -1;
    }

	return fd;
}

void UartClose(int fd)
{
    if (fd <= 0) {
        err_printf("Input err, dev fd is invalid.\n");
        return;
    }

	close(fd);
	return;
}

int UartHeaderParse(unsigned char* data, unsigned int packet_len, UartHeader* header) {
    int recv_data_len = 0;
    if (data == NULL) {
        err_printf("Input err, data ptr is NULL.\n");
        return -1;
    }

    if (packet_len < sizeof(UartHeader)) {
        err_printf("Input err, packet_len:%d is invalid.\n", packet_len);
        return -1;
    }

    memcpy(header, data, sizeof(UartHeader));
    return 0;
}

void* UartParseRawDataThread(void* arg) {
    RawData* raw_data = NULL;
    unsigned int data_len = 0;
    unsigned int total_len = 0;
    unsigned char* recv_buf = NULL;
	int ret = 0;
    unsigned int cur_len = 0;
    unsigned char* cur_pos = NULL;
    unsigned int packet_len = 0;

    if (arg == NULL) {
        err_printf("Input err, arg is NULL, UartParseRawDataThread exit.\n");
        pthread_exit((void *)0);
    }
    UartCtx* uart_ctx = (UartCtx*)arg;
    if (uart_ctx->data_buf == NULL) {
        err_printf("Uart ctx data_buf is NULL.\n");
        pthread_exit((void *)0);
    }

    while (!s_uart_ctx.quit_flag) {
        static int index = 0;
		static int packet_start = 0;
		static int start_index = 0;

        pthread_mutex_lock(&s_uart_ctx.lock);
        raw_data = OutQueue(&(uart_ctx->recv_queue));
        pthread_mutex_unlock(&s_uart_ctx.lock);
        if (raw_data == NULL) {
            usleep(1000);
            continue;
        }
        data_len = raw_data->len;

        debugw_printf("Recv data len:%u.\n", data_len);

        total_len = uart_ctx->recv_data_len + data_len;
        if (total_len > MAX_UART_DATA_BUF_LEN - 1) {
            err_printf("Data buf is full, data loss.\n");
            memset(uart_ctx->data_buf, 0, MAX_UART_DATA_BUF_LEN);
            uart_ctx->recv_data_len = 0;
            index = 0;
			packet_start = 0;
			start_index = 0;
        }

        recv_buf = &uart_ctx->data_buf[uart_ctx->recv_data_len];
        memcpy(recv_buf, raw_data->data, data_len);
        uart_ctx->recv_data_len += data_len;
        free(raw_data->data);
        free(raw_data);

        debugw_printf("Current recv data len:%d.\n", uart_ctx->recv_data_len);

        if (uart_ctx->recv_data_len < sizeof(UartHeader)) {
            debugw_printf("CUrrent recv data len:%d is less than UartHeader size, continue recv.\n");
            continue;
        }

        do {
            cur_pos = uart_ctx->data_buf + index;
            cur_len = uart_ctx->recv_data_len;

            // 找报头
            while (index < cur_len) {
                if (!packet_start && *cur_pos == HEADER_FLAG) {
					packet_start = 1;
					start_index = index;
					debugw_printf("Fetch packet start index : %d\n", index);
				} 
 
				cur_pos++;
				index++;
            }

            // 未找到报头，数据清除
			if (!packet_start) {
				memset(uart_ctx->data_buf, 0, MAX_UART_DATA_BUF_LEN);
                uart_ctx->recv_data_len = 0;
                index = 0;
				packet_start = 0;
				start_index = 0;			
				debugw_printf("Can't fetch packet start, recv_data_len %d\n", uart_ctx->recv_data_len);
				break;
			} 

            debugw_printf("Find packet, start_index:%d.\n", start_index);
            UartHeader header = {0};
            packet_len = uart_ctx->recv_data_len - start_index;
            ret = UartHeaderParse(uart_ctx->data_buf + start_index, packet_len, &header);
            if (ret != 0) {
                break;
            }
            if (packet_len < header.data_len) {
                debugw_printf("Packet recv len:%d is less than cmd data len:%d, continue data recv.\n", packet_len, header.data_len);
                break;
            }

            if (uart_ctx->packet_handler) {
                uart_ctx->packet_handler(uart_ctx->data_buf + start_index, header.data_len);
            } else {
                err_printf("Find packet, but packet_handler is NULL.\n");
            }

            // 清除已处理的报文
			uart_ctx->recv_data_len = uart_ctx->recv_data_len - start_index - header.data_len;
			memcpy(uart_ctx->data_buf, uart_ctx->data_buf + start_index + header.data_len, uart_ctx->recv_data_len);
			memset(uart_ctx->data_buf + uart_ctx->recv_data_len, 0, MAX_UART_DATA_BUF_LEN - uart_ctx->recv_data_len);
            index = 0;
			packet_start = 0;
			start_index = 0;
        } while(1);
    }

    pthread_exit((void *)0);
}

void* UartRecvDataThread(void* arg) {
    int fd = 0;
    int ret = 0;
    int que_len = 0;
    int recv_len = 0;
    char* recv_buf = NULL;

    if (NULL == arg) {
		err_printf("Uart recv data thread arg ptr is NULL.\r\n");
		pthread_exit((void *)0);
	}
    ConfUart* uart_cfg = (ConfUart*)arg;

    while (!s_uart_ctx.quit_flag) {
        if (s_uart_ctx.fd <= 0) {
            s_uart_ctx.fd = UartOpen(uart_cfg->dev_name);
            if (s_uart_ctx.fd <= 0) {
                err_printf("Uart dev open failed, sleep 3s and retry.\n");
                SendSimpleMsg(APP_PCRMGR_ID, APP_PCRMGR_ID, MSG_ID_UART_DISCONNECT);
                sleep(3);
                continue;
            } else {
                warn_printf("Uart dev open success.\n");
                SendSimpleMsg(APP_PCRMGR_ID, APP_PCRMGR_ID, MSG_ID_UART_CONNECTED);
            }
        }

        recv_buf = malloc(MAX_PACKET_LEN);
        if (recv_buf == NULL) {
            err_printf("Malloc memory failed, sleep 1s.\r\n"); 	
            sleep(1);
            continue;
        }
		bzero(recv_buf, MAX_PACKET_LEN);
        recv_len = read(s_uart_ctx.fd, recv_buf, MAX_PACKET_LEN);
        if (recv_len > 0) {
          
        } else if (recv_len == -1) {
            usleep(100000);
            free(recv_buf);
            continue;
        } else {
            pthread_mutex_lock(&s_uart_ctx.lock);
            UartClose(s_uart_ctx.fd);
            s_uart_ctx.fd = 0;
            pthread_mutex_unlock(&s_uart_ctx.lock);
            err_printf("Uart recv data err, close uart dev, sleep 1s.\n");
            free(recv_buf);
            sleep(1);
            continue;
        }

        RawData *raw_data = malloc(sizeof(RawData));
		if (raw_data == NULL) {
			err_printf("Malloc memory failed, lost data len = %d, sleep 1s.\r\n", recv_len); 	
			sleep(1);
            free(recv_buf);
			continue;
		}

		raw_data->len = recv_len;
		raw_data->data = recv_buf;
        pthread_mutex_lock(&s_uart_ctx.lock);
		ret = InQueue(&(s_uart_ctx.recv_queue), (void*)raw_data);
        que_len = GetQueueLen(&(s_uart_ctx.recv_queue));
        pthread_mutex_unlock(&s_uart_ctx.lock);		
		if (ret == false) {
			debuge_printf("Lost recv data len %d for queue full len = %d.\n", recv_len, que_len);
			free(recv_buf);
            free(raw_data);
		}

        usleep(10000);
    }
    
    pthread_exit((void *)0);
}

void* UartSendDataThread(void* arg) {
    RawData *raw_data = NULL;
    unsigned char *data_buf;
    unsigned int data_len;
    int fd = 0;
    int ret = 0;
    int try_cnt = 0;

    while (!s_uart_ctx.quit_flag) {
        pthread_mutex_lock(&s_uart_ctx.lock);
		fd = s_uart_ctx.fd;
		pthread_mutex_unlock(&s_uart_ctx.lock);

        if (s_uart_ctx.fd <= 0) {
			MillisecondsSleep(100);				
			continue;
		}

        pthread_mutex_lock(&s_uart_ctx.lock);
		raw_data = PeekQueue(&(s_uart_ctx.send_queue));
		pthread_mutex_unlock(&s_uart_ctx.lock);
		if (raw_data == NULL) {
			MillisecondsSleep(100);
			continue;
		}

        data_buf = raw_data->data;
        data_len = raw_data->len;

        while (try_cnt < RETRY_NUM) {
            ret = write(s_uart_ctx.fd, raw_data->data, raw_data->len);
            if (ret != raw_data->len) {
                try_cnt++;
                err_printf("Uart send err, write len:%d, ori data len:%d, retry cnt:%d.\n", ret, raw_data->len, try_cnt);
            } else {
                break;
            }
        }
        
        if (try_cnt == RETRY_NUM) {
            err_printf("Uart send data len:%d failed, close uart dev, sleep 1s.\n", raw_data->len);
            pthread_mutex_lock(&s_uart_ctx.lock);
            UartClose(s_uart_ctx.fd);
			s_uart_ctx.fd = 0;
			pthread_mutex_unlock(&s_uart_ctx.lock);
            sleep(1);
        } else {
            raw_data = OutQueue(&(s_uart_ctx.send_queue));
            free(raw_data->data);
            free(raw_data);
        }
    }

    pthread_exit((void *)0);
}

int DeinitUartComm() {
    s_uart_ctx.quit_flag = 1;
    UartClose(s_uart_ctx.fd);

    if (s_uart_ctx.recv_thread_id > 0) {
        pthread_join(s_uart_ctx.recv_thread_id, NULL);
    }

    if (s_uart_ctx.parse_thread_id > 0) {
        pthread_join(s_uart_ctx.parse_thread_id, NULL);
    }

    if (!s_uart_ctx.sync && s_uart_ctx.send_thread_id > 0) {
        pthread_join(s_uart_ctx.send_thread_id, NULL);
        DeinitQueue(&s_uart_ctx.send_queue);
    }
    
    DeinitQueue(&s_uart_ctx.recv_queue);

    if (s_uart_ctx.data_buf) {
        free(s_uart_ctx.data_buf);
        s_uart_ctx.data_buf = NULL;
    }
    s_uart_ctx.recv_data_len = 0;

    pthread_mutex_destroy(&s_uart_ctx.lock);

    s_uart_ctx.inited = false;
    return 0;
}

int InitUartComm(ConfUart* uart_cfg, bool sync) {
    int ret = 0;

    if (uart_cfg == NULL) {
        err_printf("Input err, uart_cfg ptr is NULL.\n");
        return -1;
    }

    memset(&s_uart_ctx, 0, sizeof(UartCtx));

    s_uart_ctx.fd = UartOpen(uart_cfg->dev_name);
    if (s_uart_ctx.fd == -1) {
        err_printf("Init uart comm failed, uart open err.\n");
        //return -1;
    }
    s_uart_ctx.sync = sync;

    pthread_mutex_init(&s_uart_ctx.lock, NULL);
    InitQueue(&(s_uart_ctx.recv_queue), MAX_DATA_QUEUE_LEN);

    s_uart_ctx.data_buf = malloc(MAX_UART_DATA_BUF_LEN);
    if (s_uart_ctx.data_buf == NULL) {
        err_printf("Init uart data buf failed, malloc mem err.\n");
        goto ERROR;
    }
    
	ret = pthread_create(&(s_uart_ctx.recv_thread_id), NULL, UartRecvDataThread, (void *)uart_cfg);
	if(ret != 0) {
		err_printf("Create RecvDataThread failed err = %d.\r\n", ret);	
		goto ERROR;
	}

    ret = pthread_create(&(s_uart_ctx.parse_thread_id), NULL, UartParseRawDataThread, (void *)&s_uart_ctx);
	if(ret != 0) {
		err_printf("Create ParseDataThread failed err = %d.\r\n", ret);	
		goto ERROR;
	}

    if (!sync) {
        ret = pthread_create(&(s_uart_ctx.send_thread_id), NULL, UartSendDataThread, NULL);
        if(ret != 0) {
            err_printf("Create SendDataThread failed err = %d.\r\n", ret);
            goto ERROR;
        }
        InitQueue(&(s_uart_ctx.send_queue), MAX_DATA_QUEUE_LEN);
    }	

    s_uart_ctx.inited = true;
    return s_uart_ctx.fd;
ERROR:
    DeinitUartComm(s_uart_ctx.fd);
    return -1;
}


int UartPacketHandlerReg(PacketHandlerFunc packer_handler) {
    if (packer_handler == NULL) {
        err_printf("Input err, packet_handler ptr is NULL.\n");
        return -1;
    }

    if (s_uart_ctx.inited == 0) {
        err_printf("Packet handler register failed, uart ctx is not inited.\n");
        return -1;
    }

    s_uart_ctx.packet_handler = packer_handler;
    return 0;
}

int UartSendAsync(unsigned char* data, unsigned int data_len) {
    int que_len = 0;
    int ret = 0;

    if (data == NULL || data_len == 0) {
        err_printf("Input err, data ptr is NULL or data_len is 0.\n");
        return -1;
    }

    if (s_uart_ctx.sync == 1) {
        err_printf("Uart fd:%d is sync, cannot send async.\n");
        return -1;
    }

    que_len = GetQueueLen(&(s_uart_ctx.send_queue));
    if (que_len == MAX_DATA_QUEUE_LEN) {
        err_printf("Uart send data len %u failed. Queue is full.\n", data_len);
        return -2;
    }

    RawData *raw_data = malloc(sizeof(RawData));
	if (raw_data == NULL) {
		err_printf("Malloc memory for RawData failed.\n", data_len);
        return -1;	
	}
    raw_data->data = malloc(data_len);
    if (raw_data == NULL) {
		err_printf("Malloc memory for data failed.\n");
        free(raw_data);
        return -1;	
	}

    memcpy(raw_data->data, data, data_len);
	raw_data->len = data_len;
	
		
	ret = InQueue(&(s_uart_ctx.send_queue), (void*)raw_data);
	que_len = GetQueueLen(&(s_uart_ctx.send_queue));
	if (ret == false) {
		debuge_printf("Uart send data len %d for queue full len = %d.\n", data_len, que_len);
		free(raw_data->data);
		free(raw_data);
	}

    return 0;
}

int UartSendSync(unsigned char* data, unsigned int data_len) {
    int que_len = 0;
    int ret = 0;
    int try_cnt = 0;

    if (data == NULL || data_len == 0) {
        err_printf("Input err, data ptr is NULL or data_len is 0.\n");
        return -1;
    }

    if (s_uart_ctx.sync == 0) {
        err_printf("Uart fd:%d is sync, cannot send sync.\n");
        return -1;
    }

    while (try_cnt < RETRY_NUM) {
        ret = write(s_uart_ctx.fd, data, data_len);
        if (ret != data_len) {
            try_cnt++;
            err_printf("Uart send err, write len:%d, ori data len:%d, retry cnt:%d.\n", ret, data_len, try_cnt);
            //usleep(250000);
        } else {
            break;
        }
    }
        
    if (try_cnt == RETRY_NUM) {
        err_printf("Uart send data len:%d failed, close uart dev, sleep 1s.\n", data_len);
		return -1;
    }

    return 0;
}
