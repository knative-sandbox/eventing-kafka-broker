// Code generated by protoc-gen-go. DO NOT EDIT.
// source: proto/def/triggers.proto

package config

import (
	fmt "fmt"
	proto "github.com/golang/protobuf/proto"
	math "math"
)

// Reference imports to suppress errors if they are not otherwise used.
var _ = proto.Marshal
var _ = fmt.Errorf
var _ = math.Inf

// This is a compile-time assertion to ensure that this generated file
// is compatible with the proto package it is being compiled against.
// A compilation error at this line likely means your copy of the
// proto package needs to be updated.
const _ = proto.ProtoPackageIsVersion3 // please upgrade the proto package

type Trigger struct {
	// attributes filters events by exact match on event context attributes.
	// Each key in the map is compared with the equivalent key in the event
	// context. An event passes the filter if all values are equal to the
	// specified values.
	//
	// Nested context attributes are not supported as keys. Only string values are supported.
	Attributes map[string]string `protobuf:"bytes,1,rep,name=attributes,proto3" json:"attributes,omitempty" protobuf_key:"bytes,1,opt,name=key,proto3" protobuf_val:"bytes,2,opt,name=value,proto3"`
	// destination is the address that receives events from the Broker that pass the Filter.
	Destination string `protobuf:"bytes,2,opt,name=destination,proto3" json:"destination,omitempty"`
	// trigger identifier
	Id                   string   `protobuf:"bytes,3,opt,name=id,proto3" json:"id,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *Trigger) Reset()         { *m = Trigger{} }
func (m *Trigger) String() string { return proto.CompactTextString(m) }
func (*Trigger) ProtoMessage()    {}
func (*Trigger) Descriptor() ([]byte, []int) {
	return fileDescriptor_3cd32e421bcc2dd3, []int{0}
}

func (m *Trigger) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_Trigger.Unmarshal(m, b)
}
func (m *Trigger) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_Trigger.Marshal(b, m, deterministic)
}
func (m *Trigger) XXX_Merge(src proto.Message) {
	xxx_messageInfo_Trigger.Merge(m, src)
}
func (m *Trigger) XXX_Size() int {
	return xxx_messageInfo_Trigger.Size(m)
}
func (m *Trigger) XXX_DiscardUnknown() {
	xxx_messageInfo_Trigger.DiscardUnknown(m)
}

var xxx_messageInfo_Trigger proto.InternalMessageInfo

func (m *Trigger) GetAttributes() map[string]string {
	if m != nil {
		return m.Attributes
	}
	return nil
}

func (m *Trigger) GetDestination() string {
	if m != nil {
		return m.Destination
	}
	return ""
}

func (m *Trigger) GetId() string {
	if m != nil {
		return m.Id
	}
	return ""
}

type Broker struct {
	Id string `protobuf:"bytes,1,opt,name=id,proto3" json:"id,omitempty"`
	// the Kafka topic to consume.
	Topic string `protobuf:"bytes,2,opt,name=topic,proto3" json:"topic,omitempty"`
	// dead letter sink URI
	DeadLetterSink string `protobuf:"bytes,3,opt,name=deadLetterSink,proto3" json:"deadLetterSink,omitempty"`
	// triggers associated with the broker
	Triggers []*Trigger `protobuf:"bytes,4,rep,name=triggers,proto3" json:"triggers,omitempty"`
	// broker namespace
	Namespace string `protobuf:"bytes,5,opt,name=namespace,proto3" json:"namespace,omitempty"`
	// broker name
	Name                 string   `protobuf:"bytes,6,opt,name=name,proto3" json:"name,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *Broker) Reset()         { *m = Broker{} }
func (m *Broker) String() string { return proto.CompactTextString(m) }
func (*Broker) ProtoMessage()    {}
func (*Broker) Descriptor() ([]byte, []int) {
	return fileDescriptor_3cd32e421bcc2dd3, []int{1}
}

func (m *Broker) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_Broker.Unmarshal(m, b)
}
func (m *Broker) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_Broker.Marshal(b, m, deterministic)
}
func (m *Broker) XXX_Merge(src proto.Message) {
	xxx_messageInfo_Broker.Merge(m, src)
}
func (m *Broker) XXX_Size() int {
	return xxx_messageInfo_Broker.Size(m)
}
func (m *Broker) XXX_DiscardUnknown() {
	xxx_messageInfo_Broker.DiscardUnknown(m)
}

var xxx_messageInfo_Broker proto.InternalMessageInfo

func (m *Broker) GetId() string {
	if m != nil {
		return m.Id
	}
	return ""
}

func (m *Broker) GetTopic() string {
	if m != nil {
		return m.Topic
	}
	return ""
}

func (m *Broker) GetDeadLetterSink() string {
	if m != nil {
		return m.DeadLetterSink
	}
	return ""
}

func (m *Broker) GetTriggers() []*Trigger {
	if m != nil {
		return m.Triggers
	}
	return nil
}

func (m *Broker) GetNamespace() string {
	if m != nil {
		return m.Namespace
	}
	return ""
}

func (m *Broker) GetName() string {
	if m != nil {
		return m.Name
	}
	return ""
}

type Brokers struct {
	Brokers []*Broker `protobuf:"bytes,1,rep,name=brokers,proto3" json:"brokers,omitempty"`
	// Count each config map update.
	// Make sure each data plane pod has the same volume generation number.
	VolumeGeneration     uint64   `protobuf:"varint,2,opt,name=volumeGeneration,proto3" json:"volumeGeneration,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *Brokers) Reset()         { *m = Brokers{} }
func (m *Brokers) String() string { return proto.CompactTextString(m) }
func (*Brokers) ProtoMessage()    {}
func (*Brokers) Descriptor() ([]byte, []int) {
	return fileDescriptor_3cd32e421bcc2dd3, []int{2}
}

func (m *Brokers) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_Brokers.Unmarshal(m, b)
}
func (m *Brokers) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_Brokers.Marshal(b, m, deterministic)
}
func (m *Brokers) XXX_Merge(src proto.Message) {
	xxx_messageInfo_Brokers.Merge(m, src)
}
func (m *Brokers) XXX_Size() int {
	return xxx_messageInfo_Brokers.Size(m)
}
func (m *Brokers) XXX_DiscardUnknown() {
	xxx_messageInfo_Brokers.DiscardUnknown(m)
}

var xxx_messageInfo_Brokers proto.InternalMessageInfo

func (m *Brokers) GetBrokers() []*Broker {
	if m != nil {
		return m.Brokers
	}
	return nil
}

func (m *Brokers) GetVolumeGeneration() uint64 {
	if m != nil {
		return m.VolumeGeneration
	}
	return 0
}

func init() {
	proto.RegisterType((*Trigger)(nil), "Trigger")
	proto.RegisterMapType((map[string]string)(nil), "Trigger.AttributesEntry")
	proto.RegisterType((*Broker)(nil), "Broker")
	proto.RegisterType((*Brokers)(nil), "Brokers")
}

func init() { proto.RegisterFile("proto/def/triggers.proto", fileDescriptor_3cd32e421bcc2dd3) }

var fileDescriptor_3cd32e421bcc2dd3 = []byte{
	// 355 bytes of a gzipped FileDescriptorProto
	0x1f, 0x8b, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0xff, 0x64, 0x92, 0x4f, 0x4b, 0xc3, 0x40,
	0x10, 0xc5, 0x49, 0xfa, 0x27, 0xed, 0x14, 0x6b, 0x59, 0x3c, 0x2c, 0xa2, 0x50, 0x8b, 0x48, 0x11,
	0xba, 0x01, 0xbd, 0x14, 0xc1, 0x83, 0x15, 0xf1, 0xe2, 0x29, 0x7a, 0x10, 0xc1, 0xc3, 0x36, 0x99,
	0x86, 0x25, 0xe9, 0x6e, 0xd8, 0x6c, 0x03, 0xfd, 0x52, 0x9e, 0xfc, 0x80, 0x92, 0x4d, 0xd2, 0x96,
	0x7a, 0x9b, 0xf9, 0xbd, 0xb7, 0x8f, 0xe1, 0xb1, 0x40, 0x33, 0xad, 0x8c, 0xf2, 0x23, 0x5c, 0xf9,
	0x46, 0x8b, 0x38, 0x46, 0x9d, 0x33, 0x8b, 0x26, 0xbf, 0x0e, 0x78, 0x1f, 0x15, 0x22, 0x73, 0x00,
	0x6e, 0x8c, 0x16, 0xcb, 0x8d, 0xc1, 0x9c, 0x3a, 0xe3, 0xd6, 0x74, 0x70, 0x47, 0x59, 0xad, 0xb2,
	0xa7, 0x9d, 0xf4, 0x22, 0x8d, 0xde, 0x06, 0x07, 0x5e, 0x32, 0x86, 0x41, 0x84, 0xb9, 0x11, 0x92,
	0x1b, 0xa1, 0x24, 0x75, 0xc7, 0xce, 0xb4, 0x1f, 0x1c, 0x22, 0x32, 0x04, 0x57, 0x44, 0xb4, 0x65,
	0x05, 0x57, 0x44, 0xe7, 0x8f, 0x70, 0x7a, 0x14, 0x48, 0x46, 0xd0, 0x4a, 0x70, 0x4b, 0x1d, 0xeb,
	0x29, 0x47, 0x72, 0x06, 0x9d, 0x82, 0xa7, 0x1b, 0xac, 0x03, 0xab, 0xe5, 0xc1, 0x9d, 0x3b, 0x93,
	0x1f, 0x07, 0xba, 0x0b, 0xad, 0x12, 0xd4, 0x75, 0xb2, 0xd3, 0x24, 0x97, 0x8f, 0x8c, 0xca, 0x44,
	0xd8, 0x3c, 0xb2, 0x0b, 0xb9, 0x81, 0x61, 0x84, 0x3c, 0x7a, 0x43, 0x63, 0x50, 0xbf, 0x0b, 0x99,
	0xd4, 0xb7, 0x1c, 0x51, 0x72, 0x0d, 0xbd, 0xa6, 0x21, 0xda, 0xb6, 0x0d, 0xf4, 0x9a, 0x06, 0x82,
	0x9d, 0x42, 0x2e, 0xa0, 0x2f, 0xf9, 0x1a, 0xf3, 0x8c, 0x87, 0x48, 0x3b, 0x36, 0x68, 0x0f, 0x08,
	0x81, 0x76, 0xb9, 0xd0, 0xae, 0x15, 0xec, 0x3c, 0xf9, 0x04, 0xaf, 0xba, 0x37, 0x27, 0x57, 0xe0,
	0x2d, 0xab, 0xb1, 0xee, 0xd8, 0x63, 0x95, 0x14, 0x34, 0x9c, 0xdc, 0xc2, 0xa8, 0x50, 0xe9, 0x66,
	0x8d, 0xaf, 0x28, 0x51, 0xef, 0x4b, 0x6d, 0x07, 0xff, 0xf8, 0xe2, 0x1b, 0x66, 0x11, 0x16, 0x2c,
	0x29, 0x8b, 0x2e, 0x90, 0x61, 0x81, 0xd2, 0x08, 0x19, 0xb3, 0x84, 0xaf, 0x12, 0xce, 0xaa, 0x44,
	0x16, 0x2a, 0x8d, 0x2c, 0x54, 0x72, 0x25, 0xe2, 0xc5, 0x49, 0x7d, 0xc8, 0xb3, 0x5d, 0xbf, 0x2e,
	0x43, 0x25, 0x8d, 0x56, 0xe9, 0x2c, 0x4b, 0xb9, 0x44, 0x3f, 0x4b, 0x62, 0xbf, 0x74, 0xfb, 0x95,
	0x7b, 0xd9, 0xb5, 0xff, 0xe4, 0xfe, 0x2f, 0x00, 0x00, 0xff, 0xff, 0x25, 0x67, 0x41, 0x91, 0x43,
	0x02, 0x00, 0x00,
}
